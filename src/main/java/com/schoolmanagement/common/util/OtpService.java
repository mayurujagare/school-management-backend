package com.schoolmanagement.common.util;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.module.auth.entity.OtpVerification;
import com.schoolmanagement.module.auth.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository    otpRepository;
    private final JavaMailSender   mailSender;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    @Value("${app.otp.rate-limit-per-15-min}")
    private int rateLimitPer15Min;

    @Value("${app.mail.from}")
    private String mailFrom;

    private final SecureRandom secureRandom = new SecureRandom();

    // ── Generate & Send OTP ───────────────────────────────────
    @Transactional
    public void generateAndSend(String identifier, String purpose) {

        // Rate limit check
        long recentCount = otpRepository.countRecentRequests(
                identifier,
                LocalDateTime.now().minusMinutes(15)
        );

        if (recentCount >= rateLimitPer15Min) {
            throw new AppException(ErrorCode.OTP_RATE_LIMIT);
        }

        String otp = generateOtp();

        OtpVerification otpVerification = OtpVerification.builder()
                .identifier(identifier)
                .otpCode(otp)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        otpRepository.save(otpVerification);

        // Send via email if identifier looks like email
        if (identifier.contains("@")) {
            sendOtpEmail(identifier, otp);
        } else {
            // SMS integration goes here later
            // smsService.send(identifier, otp);
            log.info("SMS OTP [{}] for [{}] — SMS not yet integrated", otp, identifier);
        }
    }

    // ── Verify OTP ────────────────────────────────────────────
    @Transactional
    public void verify(String identifier, String code) {

        OtpVerification otp = otpRepository
                .findLatestValid(identifier, LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        // Increment attempt count
        otp.setAttempts((short) (otp.getAttempts() + 1));

        if (otp.getAttempts() > maxAttempts) {
            otp.setIsUsed(true); // invalidate
            otpRepository.save(otp);
            throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS);
        }

        if (!otp.isValid(code)) {
            otpRepository.save(otp);
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // Mark as used
        otp.setIsUsed(true);
        otpRepository.save(otp);
    }

    // ── Private Helpers ───────────────────────────────────────
    private String generateOtp() {
        int code = 100000 + secureRandom.nextInt(900000); // always 6 digits
        return String.valueOf(code);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Your Login OTP");
            message.setText(
                "Your OTP for School Management login is: " + otp +
                "\n\nThis OTP is valid for " + otpExpiryMinutes + " minutes." +
                "\nDo not share this OTP with anyone."
            );
            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_ERROR,
                    "Failed to send OTP. Please try again.");
        }
    }
}