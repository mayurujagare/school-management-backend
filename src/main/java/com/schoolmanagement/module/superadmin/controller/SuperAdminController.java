package com.schoolmanagement.module.superadmin.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.school.dto.SchoolDto;
import com.schoolmanagement.module.school.service.SchoolService;
import com.schoolmanagement.module.superadmin.dto.SuperAdminDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")      // entire controller — super admin only
@RequiredArgsConstructor
public class SuperAdminController {

    private final SchoolService schoolService;

    // ── Platform stats dashboard ──────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SuperAdminDto.PlatformStats>> getStats() {
        return ResponseEntity.ok(
                ApiResponse.success(schoolService.getPlatformStats()));
    }

    // ── Onboard new school ────────────────────────────────────
    @PostMapping("/schools")
    public ResponseEntity<ApiResponse<SchoolDto.Response>> onboardSchool(
            @Valid @RequestBody SchoolDto.CreateRequest request) {

        SchoolDto.Response response = schoolService.onboardSchool(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("School onboarded successfully", response));
    }

    // ── List all schools ──────────────────────────────────────
    @GetMapping("/schools")
    public ResponseEntity<ApiResponse<Page<SchoolDto.Response>>> listSchools(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        schoolService.listAllSchools(search, isActive, pageable)));
    }

    // ── Get school by ID ──────────────────────────────────────
    @GetMapping("/schools/{schoolId}")
    public ResponseEntity<ApiResponse<SchoolDto.Response>> getSchool(
            @PathVariable UUID schoolId) {

        return ResponseEntity.ok(
                ApiResponse.success(schoolService.getSchoolById(schoolId)));
    }

    // ── Toggle school active/inactive ─────────────────────────
    @PatchMapping("/schools/{schoolId}/toggle-status")
    public ResponseEntity<ApiResponse<SchoolDto.Response>> toggleStatus(
            @PathVariable UUID schoolId) {

        return ResponseEntity.ok(
                ApiResponse.success(schoolService.toggleSchoolStatus(schoolId)));
    }

    // ── Update subscription ───────────────────────────────────
    @PatchMapping("/schools/{schoolId}/subscription")
    public ResponseEntity<ApiResponse<SchoolDto.Response>> updateSubscription(
            @PathVariable UUID schoolId,
            @Valid @RequestBody SchoolDto.SubscriptionUpdate request) {

        return ResponseEntity.ok(
                ApiResponse.success("Subscription updated",
                        schoolService.updateSubscription(schoolId, request)));
    }
}