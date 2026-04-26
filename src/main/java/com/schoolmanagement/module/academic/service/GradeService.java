package com.schoolmanagement.module.academic.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.dto.GradeDto;
import com.schoolmanagement.module.academic.dto.SectionDto;
import com.schoolmanagement.module.academic.dto.SubjectDto;
import com.schoolmanagement.module.academic.entity.Grade;
import com.schoolmanagement.module.academic.mapper.AcademicMapper;
import com.schoolmanagement.module.academic.repository.GradeRepository;
import com.schoolmanagement.module.academic.repository.GradeSubjectRepository;
import com.schoolmanagement.module.academic.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository       gradeRepository;
    private final SectionRepository     sectionRepository;
    private final GradeSubjectRepository gradeSubjectRepository;
    private final AcademicMapper        mapper;

    // ── Create grade ──────────────────────────────────────────
    @Transactional
    public GradeDto.Response create(GradeDto.CreateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        if (gradeRepository.existsBySchoolIdAndNameIgnoreCase(
                schoolId, request.getName())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Grade '" + request.getName() + "' already exists");
        }

        if (gradeRepository.existsBySchoolIdAndDisplayOrder(
                schoolId, request.getDisplayOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Display order " + request.getDisplayOrder() + " already in use");
        }

        Grade grade = Grade.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .displayOrder(request.getDisplayOrder())
                .build();

        return mapper.toResponse(gradeRepository.save(grade));
    }

    // ── List all active grades ────────────────────────────────
    public List<GradeDto.Response> listAll(boolean includeInactive) {
        UUID schoolId = TenantContext.getTenantId();

        List<Grade> grades = includeInactive
                ? gradeRepository.findBySchoolIdOrderByDisplayOrderAsc(schoolId)
                : gradeRepository.findBySchoolIdAndIsActiveTrueOrderByDisplayOrderAsc(schoolId);

        return grades.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get grade with its sections ───────────────────────────
    public GradeDto.Response getWithSections(UUID gradeId) {
        UUID schoolId = TenantContext.getTenantId();

        Grade grade = gradeRepository.findByIdAndSchoolId(gradeId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        List<SectionDto.Response> sections = sectionRepository
                .findByGrade_IdAndIsActiveTrueOrderByNameAsc(gradeId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        List<SubjectDto.GradeSubjectResponse> subjects = gradeSubjectRepository
                .findByGradeIdWithSubject(gradeId)
                .stream()
                .map(mapper::toGradeSubjectResponse)
                .collect(Collectors.toList());

        GradeDto.Response response = mapper.toResponse(grade);
        response.setSections(sections);
        return response;
    }

    // ── Update grade ──────────────────────────────────────────
    @Transactional
    public GradeDto.Response update(UUID gradeId, GradeDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Grade grade = gradeRepository.findByIdAndSchoolId(gradeId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        if (request.getName() != null) {
            if (!request.getName().equalsIgnoreCase(grade.getName()) &&
                    gradeRepository.existsBySchoolIdAndNameIgnoreCase(
                            schoolId, request.getName())) {
                throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                        "Grade name already exists");
            }
            grade.setName(request.getName());
        }

        if (request.getDisplayOrder() != null)
            grade.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null)
            grade.setIsActive(request.getIsActive());

        return mapper.toResponse(gradeRepository.save(grade));
    }

    // ── Helper: validate grade belongs to tenant ──────────────
    public Grade validateAndGet(UUID gradeId, UUID schoolId) {
        return gradeRepository.findByIdAndSchoolId(gradeId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));
    }
}