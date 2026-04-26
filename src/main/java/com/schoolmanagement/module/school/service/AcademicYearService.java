// src/main/java/com/schoolmanagement/module/school/service/AcademicYearService.java
package com.schoolmanagement.module.school.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.school.dto.AcademicYearDto;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.mapper.SchoolMapper;
import com.schoolmanagement.module.school.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final SchoolMapper           mapper;

    // ── Create academic year ───────────────────────────────────
    @Transactional
    public AcademicYearDto.Response create(AcademicYearDto.CreateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        if (academicYearRepository.existsBySchoolIdAndLabel(
                schoolId, request.getLabel())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Academic year '" + request.getLabel() + "' already exists");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "End date must be after start date");
        }

        // Unset current flag if setting this as current
        if (Boolean.TRUE.equals(request.getSetAsCurrent())) {
            academicYearRepository.unsetCurrentForSchool(schoolId);
        }

        AcademicYear year = AcademicYear.builder()
                .schoolId(schoolId)
                .label(request.getLabel())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(Boolean.TRUE.equals(request.getSetAsCurrent()))
                .build();

        return mapper.toResponse(academicYearRepository.save(year));
    }

    // ── List all academic years for school ────────────────────
    public List<AcademicYearDto.Response> listAll() {
        UUID schoolId = TenantContext.getTenantId();
        return academicYearRepository
                .findBySchoolIdOrderByStartDateDesc(schoolId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get current academic year ─────────────────────────────
    public AcademicYearDto.Response getCurrent() {
        UUID schoolId = TenantContext.getTenantId();
        return academicYearRepository
                .findBySchoolIdAndIsCurrentTrue(schoolId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "No current academic year set"));
    }

    // ── Set as current ────────────────────────────────────────
    @Transactional
    public AcademicYearDto.Response setAsCurrent(UUID yearId) {
        UUID schoolId = TenantContext.getTenantId();

        AcademicYear year = academicYearRepository.findById(yearId)
                .filter(y -> y.getSchoolId().equals(schoolId))  // tenant check
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Academic year not found"));

        academicYearRepository.unsetCurrentForSchool(schoolId);
        year.setIsCurrent(true);

        return mapper.toResponse(academicYearRepository.save(year));
    }

    // ── Helper: get current year entity (used by other services) ──
    public AcademicYear getCurrentYearEntity(UUID schoolId) {
        return academicYearRepository
                .findBySchoolIdAndIsCurrentTrue(schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "No current academic year configured for this school"));
    }
}