package com.schoolmanagement.module.announcement.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.announcement.dto.AnnouncementDto;
import com.schoolmanagement.module.announcement.entity.Announcement.TargetType;
import com.schoolmanagement.module.announcement.service.AnnouncementService;
import com.schoolmanagement.module.announcement.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService      announcementService;
    private final PushNotificationService  pushNotificationService;

    // ─────────────────────────────────────────────────────────
    //  ANNOUNCEMENT CRUD
    // ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    public ResponseEntity<ApiResponse<AnnouncementDto.Response>> create(
            @Valid @RequestBody AnnouncementDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Announcement created",
                        announcementService.create(request)));
    }

    @PatchMapping("/{announcementId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    public ResponseEntity<ApiResponse<AnnouncementDto.Response>> update(
            @PathVariable UUID announcementId,
            @Valid @RequestBody AnnouncementDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Announcement updated",
                        announcementService.update(announcementId, request)));
    }

    @PostMapping("/{announcementId}/publish")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<AnnouncementDto.Response>> publish(
            @PathVariable UUID announcementId) {

        return ResponseEntity.ok(
                ApiResponse.success("Announcement published",
                        announcementService.publish(announcementId)));
    }

    @DeleteMapping("/{announcementId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @PathVariable UUID announcementId) {

        announcementService.deleteDraft(announcementId);
        return ResponseEntity.ok(
                ApiResponse.success("Draft deleted"));
    }

    @GetMapping("/{announcementId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<AnnouncementDto.Response>> getById(
            @PathVariable UUID announcementId) {

        return ResponseEntity.ok(
                ApiResponse.success(announcementService.getById(announcementId)));
    }

    // ─────────────────────────────────────────────────────────
    //  LIST / FILTER
    // ─────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<Page<AnnouncementDto.Response>>> listAll(
            @RequestParam(required = false) TargetType targetType,
            @RequestParam(required = false) Boolean isDraft,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        announcementService.listAll(targetType, isDraft, pageable)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<AnnouncementDto.Stats>> getStats() {

        return ResponseEntity.ok(
                ApiResponse.success(announcementService.getStats()));
    }

    // ─────────────────────────────────────────────────────────
    //  PARENT FEED
    // ─────────────────────────────────────────────────────────

    @GetMapping("/feed")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Page<AnnouncementDto.Response>>> parentFeed(
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        announcementService.getParentFeed(currentUserId, pageable)));
    }

    // ─────────────────────────────────────────────────────────
    //  DEVICE TOKENS (Mobile App)
    // ─────────────────────────────────────────────────────────

    @PostMapping("/devices/register")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            @AuthenticationPrincipal UUID currentUserId,
            @Valid @RequestBody AnnouncementDto.RegisterDeviceRequest request) {

        pushNotificationService.registerToken(currentUserId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Device registered for notifications"));
    }

    @PostMapping("/devices/unregister")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Void>> unregisterDevice(
            @AuthenticationPrincipal UUID currentUserId,
            @RequestParam String token) {

        pushNotificationService.removeToken(currentUserId, token);
        return ResponseEntity.ok(
                ApiResponse.success("Device unregistered"));
    }
}