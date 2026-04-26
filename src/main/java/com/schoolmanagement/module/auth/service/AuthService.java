// src/main/java/com/schoolmanagement/module/auth/service/AuthService.java
package com.schoolmanagement.module.auth.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.util.JwtUtil;
import com.schoolmanagement.common.util.OtpService;
import com.schoolmanagement.module.auth.dto.AuthRequest;
import com.schoolmanagement.module.auth.dto.AuthResponse;
import com.schoolmanagement.module.auth.entity.RefreshToken;
import com.schoolmanagement.module.auth.entity.Role;
import com.schoolmanagement.module.auth.entity.User;
import com.schoolmanagement.module.auth.repository.RefreshTokenRepository;
import com.schoolmanagement.module.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService            otpService;
    private final JwtUtil               jwtUtil;
    private final PasswordEncoder       passwordEncoder;

    @Value("${app.jwt.refresh-token-expiry-days}")
    private int refreshTokenExpiryDays;

    // ── Parent: Request OTP ───────────────────────────────────
    public void requestOtp(AuthRequest.OtpRequest request) {

        String identifier = request.getIdentifier().trim().toLowerCase();

        // Confirm user exists and is a parent
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.IDENTIFIER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        if (!user.hasRole(Role.PARENT)) {
            throw new AppException(ErrorCode.UNAUTHORIZED,
                    "OTP login is only available for parent accounts");
        }

        otpService.generateAndSend(identifier, "LOGIN");
    }

    // ── Parent: Verify OTP & Issue Tokens ────────────────────
    @Transactional
    public AuthResponse verifyOtp(AuthRequest.OtpVerify request,
                                   HttpServletRequest httpRequest) {

        String identifier = request.getIdentifier().trim().toLowerCase();

        otpService.verify(identifier, request.getOtp());

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return issueTokens(user, httpRequest);
    }

    // ── Staff: Password Login ─────────────────────────────────
    @Transactional
    public AuthResponse staffLogin(AuthRequest.StaffLogin request,
                                    HttpServletRequest httpRequest) {

        String identifier = request.getIdentifier().trim().toLowerCase();

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        if (user.getAuthType() == User.AuthType.OTP) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user, httpRequest);
    }

    // ── Refresh Access Token ──────────────────────────────────
    @Transactional
    public AuthResponse refresh(AuthRequest.RefreshRequest request,
                                 HttpServletRequest httpRequest) {

        String tokenHash = jwtUtil.hashToken(request.getRefreshToken());

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (!stored.isValid()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // Rotate refresh token (security best practice)
        stored.setIsRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser(), httpRequest);
    }

    // ── Logout ────────────────────────────────────────────────
    @Transactional
    public void logout(AuthRequest.RefreshRequest request) {
        String tokenHash = jwtUtil.hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(t -> {
                    t.setIsRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    // ── Logout All Devices ────────────────────────────────────
    @Transactional
    public void logoutAll(User currentUser) {
        refreshTokenRepository.revokeAllByUserId(currentUser.getId());
    }

    // ── Change Password ───────────────────────────────────────
    @Transactional
    public void changePassword(User currentUser,
                                AuthRequest.ChangePassword request) {

        if (!passwordEncoder.matches(request.getCurrentPassword(),
                currentUser.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS,
                    "Current password is incorrect");
        }

        currentUser.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }

    // ── Private: Issue Tokens & Build Response ────────────────
    private AuthResponse issueTokens(User user, HttpServletRequest httpRequest) {

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Issue access token (JWT)
        String accessToken = jwtUtil.generateAccessToken(user);

        // Issue refresh token (opaque, stored hashed)
        String rawRefreshToken = jwtUtil.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtUtil.hashToken(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .deviceInfo(httpRequest.getHeader("User-Agent"))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .schoolId(user.getSchoolId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .mobile(user.getMobile())
                        .roles(user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }
}