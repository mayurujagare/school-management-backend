package com.schoolmanagement.module.academic.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.dto.SubjectDto;
import com.schoolmanagement.module.academic.entity.Grade;
import com.schoolmanagement.module.academic.entity.GradeSubject;
import com.schoolmanagement.module.academic.entity.Subject;
import com.schoolmanagement.module.academic.mapper.AcademicMapper;
import com.schoolmanagement.module.academic.repository.GradeRepository;
import com.schoolmanagement.module.academic.repository.GradeSubjectRepository;
import com.schoolmanagement.module.academic.repository.SubjectRepository;
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
public class SubjectService {

    private final SubjectRepository      subjectRepository;
    private final GradeRepository        gradeRepository;
    private final GradeSubjectRepository gradeSubjectRepository;
    private final AcademicMapper         mapper;

    // ── Create subject ────────────────────────────────────────
    @Transactional
    public SubjectDto.Response create(SubjectDto.CreateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        if (subjectRepository.existsBySchoolIdAndNameIgnoreCase(
                schoolId, request.getName())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Subject '" + request.getName() + "' already exists");
        }

        if (request.getCode() != null &&
                subjectRepository.existsBySchoolIdAndCodeIgnoreCase(
                        schoolId, request.getCode())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Subject code '" + request.getCode() + "' already in use");
        }

        Subject subject = Subject.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .code(request.getCode() != null
                        ? request.getCode().toUpperCase() : null)
                .build();

        return mapper.toResponse(subjectRepository.save(subject));
    }

    // ── List all subjects ─────────────────────────────────────
    public List<SubjectDto.Response> listAll(boolean includeInactive) {
        UUID schoolId = TenantContext.getTenantId();

        List<Subject> subjects = includeInactive
                ? subjectRepository.findBySchoolIdOrderByNameAsc(schoolId)
                : subjectRepository.findBySchoolIdAndIsActiveTrueOrderByNameAsc(schoolId);

        return subjects.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Update subject ────────────────────────────────────────
    @Transactional
    public SubjectDto.Response update(UUID subjectId,
                                       SubjectDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Subject subject = subjectRepository
                .findByIdAndSchoolId(subjectId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Subject not found"));

        if (request.getName() != null) {
            if (!request.getName().equalsIgnoreCase(subject.getName()) &&
                    subjectRepository.existsBySchoolIdAndNameIgnoreCase(
                            schoolId, request.getName())) {
                throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                        "Subject name already exists");
            }
            subject.setName(request.getName());
        }

        if (request.getCode() != null) {
            if (!request.getCode().equalsIgnoreCase(subject.getCode()) &&
                    subjectRepository.existsBySchoolIdAndCodeIgnoreCase(
                            schoolId, request.getCode())) {
                throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                        "Subject code already in use");
            }
            subject.setCode(request.getCode().toUpperCase());
        }

        if (request.getIsActive() != null) subject.setIsActive(request.getIsActive());

        return mapper.toResponse(subjectRepository.save(subject));
    }

    // ── Assign subject to grade ───────────────────────────────
    @Transactional
    public void assignToGrade(UUID gradeId, UUID subjectId,
                               SubjectDto.AssignToGradeRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Grade grade = gradeRepository.findByIdAndSchoolId(gradeId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        Subject subject = subjectRepository
                .findByIdAndSchoolId(subjectId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Subject not found"));

        if (gradeSubjectRepository.existsByGrade_IdAndSubject_Id(
                gradeId, subjectId)) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Subject already assigned to this grade");
        }

        GradeSubject gradeSubject = GradeSubject.builder()
                .grade(grade)
                .subject(subject)
                .isMandatory(request.getIsMandatory())
                .build();

        gradeSubjectRepository.save(gradeSubject);
        log.info("Assigned subject [{}] to grade [{}]",
                subject.getName(), grade.getName());
    }

    // ── Remove subject from grade ─────────────────────────────
    @Transactional
    public void removeFromGrade(UUID gradeId, UUID subjectId) {
        UUID schoolId = TenantContext.getTenantId();

        // Validate both belong to this school
        gradeRepository.findByIdAndSchoolId(gradeId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        gradeSubjectRepository.deleteByGradeIdAndSubjectId(gradeId, subjectId);
    }

    // ── List subjects assigned to a grade ─────────────────────
    public List<SubjectDto.GradeSubjectResponse> listByGrade(UUID gradeId) {
        UUID schoolId = TenantContext.getTenantId();

        gradeRepository.findByIdAndSchoolId(gradeId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        return gradeSubjectRepository
                .findByGradeIdWithSubject(gradeId)
                .stream()
                .map(mapper::toGradeSubjectResponse)
                .collect(Collectors.toList());
    }
}