package com.schoolmanagement.module.school.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolmanagement.module.school.entity.School;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class SchoolDto {

    // ── Create school (Super Admin) ───────────────────────────
    @Data
    public static class CreateRequest {

        @NotBlank(message = "School name is required")
        private String name;

        private String address;
        private String city;
        private String state;
        private String pincode;
        private String phone;

        @Email(message = "Invalid email format")
        private String email;

        private String website;

        @NotBlank(message = "Admin first name is required")
        private String adminFirstName;

        @NotBlank(message = "Admin last name is required")
        private String adminLastName;

        @NotBlank(message = "Admin email or mobile is required")
        private String adminIdentifier;  // email or mobile for admin login

        @NotNull(message = "Subscription plan is required")
        private School.SubscriptionPlan subscriptionPlan;

        private LocalDate subscriptionExpiry;
    }

    // ── Update school details (School Admin / Super Admin) ────
    @Data
    public static class UpdateRequest {
        private String name;
        private String address;
        private String city;
        private String state;
        private String pincode;
        private String phone;

        @Email(message = "Invalid email format")
        private String email;

        private String website;
        private String logoUrl;
    }

    // ── School response ───────────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID                    id;
        private String                  name;
        private String                  address;
        private String                  city;
        private String                  state;
        private String                  country;
        private String                  pincode;
        private String                  phone;
        private String                  email;
        private String                  logoUrl;
        private String                  website;
        private Boolean                 isActive;
        private School.SubscriptionPlan subscriptionPlan;
        private LocalDate               subscriptionStart;
        private LocalDate               subscriptionExpiry;
        private Boolean                 subscriptionActive;
        private LocalDateTime           createdAt;
    }

    // ── Subscription update (Super Admin only) ────────────────
    @Data
    public static class SubscriptionUpdate {

        @NotNull(message = "Subscription plan is required")
        private School.SubscriptionPlan plan;

        @NotNull(message = "Expiry date is required")
        private LocalDate expiryDate;
    }
}