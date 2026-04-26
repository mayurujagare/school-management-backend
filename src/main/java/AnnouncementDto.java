package com.schoolmanagement.module.announcement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolmanagement.module.announcement.entity.Announcement.TargetType;
import com.schoolmanagement.module.announcement.entity.DeviceToken.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnnouncementDto {

    // ── Create announcement (draft or publish immediately) ────
    @Data
    public static class CreateRequest {

        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Body is required")
        private String body;

        @NotNull(message = "Target type is required")
        private TargetType targetType;     // ALL, GRADE, SECTION

        private UUID targetId;             // required if GRADE or SECTION

        private Boolean publishNow = false; // true = publish + send push immediately
    }

    // ── Update announcement (only drafts can be updated) ──────
    @Data
    public static class UpdateRequest {
        private String     title;
        private String     body;
        private TargetType targetType;
        private UUID       targetId;
    }

    // ── Announcement response ─────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID          id;
        private String        title;
        private String        body;
        private TargetType    targetType;
        private UUID          targetId;
        private String        targetName;      // resolved grade/section name
        private boolean       isDraft;
        private LocalDateTime publishedAt;
        private String        createdByName;
        private LocalDateTime createdAt;
    }

    // ── Register device token (mobile app) ────────────────────
    @Data
    public static class RegisterDeviceRequest {

        @NotBlank(message = "Device token is required")
        private String token;

        @NotNull(message = "Platform is required")
        private Platform platform;          // ANDROID or IOS
    }

    // ── Push notification payload ─────────────────────────────
    @Data
    @Builder
    public static class PushPayload {
        private String title;
        private String body;
        private UUID   announcementId;
        private String targetType;
    }

    // ── Announcement stats (for dashboard) ────────────────────
    @Data
    @Builder
    public static class Stats {
        private long totalAnnouncements;
        private long drafts;
        private long published;
    }
}