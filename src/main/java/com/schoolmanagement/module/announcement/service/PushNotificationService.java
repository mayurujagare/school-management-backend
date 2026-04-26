package com.schoolmanagement.module.announcement.service;

import com.schoolmanagement.module.announcement.dto.AnnouncementDto;
import com.schoolmanagement.module.announcement.entity.Announcement;
import com.schoolmanagement.module.announcement.entity.Announcement.TargetType;
import com.schoolmanagement.module.announcement.entity.DeviceToken;
import com.schoolmanagement.module.announcement.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    // ── Register device token ─────────────────────────────────
    public void registerToken(UUID userId, AnnouncementDto.RegisterDeviceRequest request) {

        // Upsert — update if exists, create if new
        DeviceToken token = deviceTokenRepository
                .findByUserIdAndToken(userId, request.getToken())
                .orElse(DeviceToken.builder()
                        .userId(userId)
                        .token(request.getToken())
                        .platform(request.getPlatform())
                        .build());

        token.setIsActive(true);
        token.setPlatform(request.getPlatform());
        deviceTokenRepository.save(token);

        log.info("Device token registered for user [{}] platform [{}]",
                userId, request.getPlatform());
    }

    // ── Remove device token (logout) ──────────────────────────
    public void removeToken(UUID userId, String token) {
        deviceTokenRepository.deactivateToken(userId, token);
        log.info("Device token deactivated for user [{}]", userId);
    }

    // ── Remove all tokens for user ────────────────────────────
    public void removeAllTokens(UUID userId) {
        deviceTokenRepository.deactivateAllForUser(userId);
        log.info("All device tokens deactivated for user [{}]", userId);
    }

    // ── Send push notification for announcement ───────────────
    @Async
    public void sendPushForAnnouncement(Announcement announcement) {

        List<DeviceToken> tokens = resolveTargetTokens(announcement);

        if (tokens.isEmpty()) {
            log.info("No active device tokens found for announcement [{}]",
                    announcement.getId());
            return;
        }

        AnnouncementDto.PushPayload payload = AnnouncementDto.PushPayload.builder()
                .title(announcement.getTitle())
                .body(truncateBody(announcement.getBody(), 200))
                .announcementId(announcement.getId())
                .targetType(announcement.getTargetType().name())
                .build();

        log.info("Sending push notification for announcement [{}] to {} devices",
                announcement.getId(), tokens.size());

        for (DeviceToken deviceToken : tokens) {
            try {
                sendToDevice(deviceToken, payload);
            } catch (Exception e) {
                log.error("Failed to send push to device [{}]: {}",
                        deviceToken.getId(), e.getMessage());
                // Mark token as inactive if delivery fails
                deviceToken.setIsActive(false);
                deviceTokenRepository.save(deviceToken);
            }
        }

        log.info("Push notification completed for announcement [{}]",
                announcement.getId());
    }

    // ── Private: resolve which tokens to send to ──────────────
    private List<DeviceToken> resolveTargetTokens(Announcement announcement) {
        return switch (announcement.getTargetType()) {
            case ALL     -> deviceTokenRepository
                    .findActiveTokensBySchoolId(announcement.getSchoolId());
            case GRADE   -> deviceTokenRepository
                    .findActiveTokensByGradeId(announcement.getTargetId());
            case SECTION -> deviceTokenRepository
                    .findActiveTokensBySectionId(announcement.getTargetId());
        };
    }

    // ── Private: send to single device ────────────────────────
    // TODO: Replace with actual FCM/Firebase integration
    private void sendToDevice(DeviceToken deviceToken,
                               AnnouncementDto.PushPayload payload) {

        // ───────────────────────────────────────────────────────
        // PLACEHOLDER — Replace with Firebase Cloud Messaging
        //
        // When you integrate FCM:
        // 1. Add firebase-admin dependency to pom.xml
        // 2. Initialize FirebaseApp with service account JSON
        // 3. Use FirebaseMessaging.getInstance().send(message)
        //
        // Example:
        // Message message = Message.builder()
        //     .setToken(deviceToken.getToken())
        //     .setNotification(Notification.builder()
        //         .setTitle(payload.getTitle())
        //         .setBody(payload.getBody())
        //         .build())
        //     .putData("announcementId", payload.getAnnouncementId().toString())
        //     .putData("targetType", payload.getTargetType())
        //     .build();
        //
        // String response = FirebaseMessaging.getInstance().send(message);
        // ───────────────────────────────────────────────────────

        log.debug("PUSH [{}] → {} | title: {} | platform: {}",
                deviceToken.getUserId(),
                deviceToken.getToken().substring(0, Math.min(20, deviceToken.getToken().length())) + "...",
                payload.getTitle(),
                deviceToken.getPlatform());
    }

    // ── Private: truncate body for push preview ───────────────
    private String truncateBody(String body, int maxLength) {
        if (body == null) return "";
        if (body.length() <= maxLength) return body;
        return body.substring(0, maxLength - 3) + "...";
    }
}