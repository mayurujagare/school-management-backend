package com.schoolmanagement.module.staff.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.staff.dto.StaffDto;
import com.schoolmanagement.module.staff.service.StaffService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    // ── Create staff member ────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<StaffDto.Response>> create(
            @Valid @RequestBody StaffDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff created successfully",
                        staffService.create(request)));
    }

    // ── Search staff ───────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<Page<StaffDto.Response>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        staffService.search(search, isActive, pageable)));
    }

    // ── Get staff by ID ────────────────────────────────────────
    @GetMapping("/{staffId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<StaffDto.Response>> getById(
            @PathVariable UUID staffId) {

        return ResponseEntity.ok(
                ApiResponse.success(staffService.getById(staffId)));
    }

    // ── Update staff ───────────────────────────────────────────
    @PatchMapping("/{staffId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<StaffDto.Response>> update(
            @PathVariable UUID staffId,
            @Valid @RequestBody StaffDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Staff updated",
                        staffService.update(staffId, request)));
    }

    // ── Assign to section ──────────────────────────────────────
    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<StaffDto.SectionTeacherResponse>> assign(
            @Valid @RequestBody StaffDto.AssignRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Staff assigned to section",
                        staffService.assignToSection(request)));
    }

    // ── Get section's teachers ─────────────────────────────────
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<List<StaffDto.SectionTeacherResponse>>>
            getSectionTeachers(@PathVariable UUID sectionId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        staffService.getSectionTeachers(sectionId)));
    }

    // ── Get teacher's assignments ──────────────────────────────
    @GetMapping("/{staffId}/assignments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    public ResponseEntity<ApiResponse<List<StaffDto.SectionTeacherResponse>>>
            getAssignments(@PathVariable UUID staffId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        staffService.getTeacherAssignments(staffId)));
    }

    // ── Remove assignment ──────────────────────────────────────
    @DeleteMapping("/section/{sectionId}/staff/{staffId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(
            @PathVariable UUID sectionId,
            @PathVariable UUID staffId) {

        staffService.removeAssignment(sectionId, staffId);
        return ResponseEntity.ok(
                ApiResponse.success("Assignment removed"));
    }

    // ── My profile (logged-in staff) ───────────────────────────
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('TEACHER','CLERK','PRINCIPAL','SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<StaffDto.Response>> myProfile(
            @AuthenticationPrincipal UUID currentUserId) {

        return ResponseEntity.ok(
                ApiResponse.success(staffService.getMyProfile(currentUserId)));
    }
}