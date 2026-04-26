package com.schoolmanagement.module.academic.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.dto.SectionDto;
import com.schoolmanagement.module.academic.entity.Grade;
import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.mapper.AcademicMapper;
import com.schoolmanagement.module.academic.repository.GradeRepository;
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
public class SectionService {

    private final SectionRepository sectionRepository;
    private final GradeRepository   gradeRepository;
    private final AcademicMapper    mapper;

    // ── Create section ────────────────────────────────────────
    @Transactional
    public SectionDto.Response create(SectionDto.CreateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        // Validate grade belongs to this school
        Grade grade = gradeRepository
                .findByIdAndSchoolId(request.getGradeId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        if (sectionRepository.existsByGrade_IdAndNameIgnoreCase(
                request.getGradeId(), request.getName())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Section '" + request.getName() +
                    "' already exists in " + grade.getName());
        }

        Section section = Section.builder()
                .schoolId(schoolId)
                .grade(grade)
                .name(request.getName().toUpperCase())
                .capacity(request.getCapacity())
                .build();

        return mapper.toResponse(sectionRepository.save(section));
    }

    // ── List sections by grade ────────────────────────────────
    public List<SectionDto.Response> listByGrade(UUID gradeId) {
        UUID schoolId = TenantContext.getTenantId();

        // Validate grade belongs to this school
        gradeRepository.findByIdAndSchoolId(gradeId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        return sectionRepository
                .findByGrade_IdAndIsActiveTrueOrderByNameAsc(gradeId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── List all sections for school ──────────────────────────
    public List<SectionDto.Response> listAll() {
        UUID schoolId = TenantContext.getTenantId();
        return sectionRepository
                .findAllWithGradeBySchoolId(schoolId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Update section ────────────────────────────────────────
    @Transactional
    public SectionDto.Response update(UUID sectionId,
                                       SectionDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Section section = sectionRepository
                .findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        if (request.getName() != null) {
            if (!request.getName().equalsIgnoreCase(section.getName()) &&
                    sectionRepository.existsByGrade_IdAndNameIgnoreCase(
                            section.getGrade().getId(), request.getName())) {
                throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                        "Section name already exists in this grade");
            }
            section.setName(request.getName().toUpperCase());
        }

        if (request.getCapacity() != null) section.setCapacity(request.getCapacity());
        if (request.getIsActive()  != null) section.setIsActive(request.getIsActive());

        return mapper.toResponse(sectionRepository.save(section));
    }

    // ── Helper: validate section belongs to tenant ────────────
    public Section validateAndGet(UUID sectionId, UUID schoolId) {
        return sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));
    }
}