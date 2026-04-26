package com.schoolmanagement.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthRequest {

    // Parent OTP — Step 1: request OTP
    @Data
    public static class OtpRequest {
        @NotBlank(message = "Email or mobile is required")
        private String identifier;   // email or mobile
    }

    // Parent OTP — Step 2: verify OTP
    @Data
    public static class OtpVerify {
        @NotBlank(message = "Email or mobile is required")
        private String identifier;

        @NotBlank(message = "OTP is required")
        private String otp;
    }

    // Staff — password login
    @Data
    public static class StaffLogin {
        @NotBlank(message = "Email or mobile is required")
        private String identifier;

        @NotBlank(message = "Password is required")
        private String password;
    }

    // Refresh token
    @Data
    public static class RefreshRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    // Change password (first login or reset)
    @Data
    public static class ChangePassword {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        private String newPassword;
    }
}