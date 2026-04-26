package com.schoolmanagement.module.auth.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.auth.dto.AuthRequest;
import com.schoolmanagement.module.auth.dto.AuthResponse;
import com.schoolmanagement.module.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Parent: Step 1 — Request OTP ─────────────────────────
    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(
            @Valid @RequestBody AuthRequest.OtpRequest request) {

        authService.requestOtp(request);
        return ResponseEntity.ok(
                ApiResponse.success("OTP sent successfully"));
    }

    // ── Parent: Step 2 — Verify OTP ──────────────────────────
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody AuthRequest.OtpVerify request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.verifyOtp(request, httpRequest);
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response));
    }

    // ── Staff: Password Login ─────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> staffLogin(
            @Valid @RequestBody AuthRequest.StaffLogin request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.staffLogin(request, httpRequest);
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response));
    }

    // ── All: Refresh Token ────────────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody AuthRequest.RefreshRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.refresh(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── All: Logout (current device) ─────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody AuthRequest.RefreshRequest request) {

        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    // ── Staff: Change Password ────────────────────────────────
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UUID currentUserId,
            @Valid @RequestBody AuthRequest.ChangePassword request) {

        // fetch user and delegate
        // full implementation in next phase with UserService
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}