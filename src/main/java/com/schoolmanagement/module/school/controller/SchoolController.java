package com.schoolmanagement.module.school.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.school.dto.AcademicYearDto;
import com.schoolmanagement.module.school.dto.SchoolConfigDto;
import com.schoolmanagement.module.school.dto.SchoolDto;
import com.schoolmanagement.module.school.service.AcademicYearService;
import com.schoolmanagement.module.school.service.SchoolConfigService;
import com.schoolmanagement.module.school.service.SchoolService;
import com.schoolmanagement.common.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService       schoolService;
    private final SchoolConfigService configService;
    private final AcademicYearService academicYearService;

    // ─────────────────────────────────────────────────────────
    //  SCHOOL PROFILE
    // ─────────────────────────────────────────────────────────

    // Get own school profile
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<SchoolDto.Response>> getMySchool() {
        return ResponseEntity.ok(
                ApiResponse.success(schoolService.getMySchool()));
    }

    // Update own school profile
    @PatchMapping("/me")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<SchoolDto.Response>> updateMySchool(
            @Valid @RequestBody SchoolDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("School updated successfully",
                        schoolService.updateMySchool(request)));
    }

    // ─────────────────────────────────────────────────────────
    //  SCHOOL CONFIG
    // ─────────────────────────────────────────────────────────

    // Get all configs
    @GetMapping("/me/config")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfigs() {
        UUID schoolId = TenantContext.getTenantId();
        return ResponseEntity.ok(
                ApiResponse.success(configService.getAllConfigs(schoolId)));
    }

    // Set single config
    @PostMapping("/me/config")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<Void>> setConfig(
            @Valid @RequestBody SchoolConfigDto.SetRequest request) {

        UUID schoolId = TenantContext.getTenantId();
        configService.setConfig(schoolId, request.getKey(), request.getValue());
        return ResponseEntity.ok(ApiResponse.success("Config updated"));
    }

    // Bulk set configs
    @PostMapping("/me/config/bulk")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<Void>> bulkSetConfigs(
            @Valid @RequestBody SchoolConfigDto.BulkSetRequest request) {

        UUID schoolId = TenantContext.getTenantId();
        configService.bulkSetConfigs(schoolId, request.getConfigs());
        return ResponseEntity.ok(ApiResponse.success("Configs updated"));
    }

    // ─────────────────────────────────────────────────────────
    //  ACADEMIC YEARS
    // ─────────────────────────────────────────────────────────

    // List all academic years
    @GetMapping("/me/academic-years")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<List<AcademicYearDto.Response>>> listAcademicYears() {
        return ResponseEntity.ok(
                ApiResponse.success(academicYearService.listAll()));
    }

    // Get current academic year
    @GetMapping("/me/academic-years/current")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<AcademicYearDto.Response>> getCurrentYear() {
        return ResponseEntity.ok(
                ApiResponse.success(academicYearService.getCurrent()));
    }

    // Create academic year
    @PostMapping("/me/academic-years")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<AcademicYearDto.Response>> createAcademicYear(
            @Valid @RequestBody AcademicYearDto.CreateRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Academic year created",
                        academicYearService.create(request)));
    }

    // Set academic year as current
    @PatchMapping("/me/academic-years/{yearId}/set-current")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<AcademicYearDto.Response>> setCurrentYear(
            @PathVariable UUID yearId) {

        return ResponseEntity.ok(
                ApiResponse.success("Current academic year updated",
                        academicYearService.setAsCurrent(yearId)));
    }
}