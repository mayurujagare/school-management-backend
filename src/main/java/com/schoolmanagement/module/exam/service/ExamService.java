package com.schoolmanagement.module.exam.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.entity.Grade;
import com.schoolmanagement.module.academic.entity.Subject;
import com.schoolmanagement.module.academic.repository.GradeRepository;
import com.schoolmanagement.module.academic.repository.SubjectRepository;
import com.schoolmanagement.module.exam.dto.ExamDto;
import com.schoolmanagement.module.exam.entity.Exam;
import com.schoolmanagement.module.exam.entity.ExamType;
import com.schoolmanagement.module.exam.entity.GradingScale;
import com.schoolmanagement.module.exam.mapper.ExamMapper;
import com.schoolmanagement.module.exam.repository.ExamRepository;
import com.schoolmanagement.module.exam.repository.ExamTypeRepository;
import com.schoolmanagement.module.exam.repository.GradingScaleRepository;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.service.AcademicYearService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository           examRepository;
    private final ExamTypeRepository       examTypeRepository;
    private final GradeRepository          gradeRepository;
    private final SubjectRepository        subjectRepository;
    private final GradingScaleRepository   gradingScaleRepository;
    private final AcademicYearService      academicYearService;
    private final ExamMapper               mapper;

    @Transactional
    public ExamDto.Response create(ExamDto.CreateRequest request) {
        UUID schoolId  = TenantContext.getTenantId();
        UUID createdBy = getCurrentUserId();

        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        ExamType examType = examTypeRepository
                .findByIdAndSchoolId(request.getExamTypeId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Exam type not found"));

        Grade grade = gradeRepository
                .findByIdAndSchoolId(request.getGradeId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        Subject subject = subjectRepository
                .findByIdAndSchoolId(request.getSubjectId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Subject not found"));

        if (examRepository
                .existsByAcademicYear_IdAndExamType_IdAndGrade_IdAndSubject_Id(
                        year.getId(), examType.getId(),
                        grade.getId(), subject.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Exam already exists for this combination");
        }

        GradingScale scale = null;
        if (request.getGradingScaleId() != null) {
            scale = gradingScaleRepository
                    .findByIdAndSchoolId(request.getGradingScaleId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                            "Grading scale not found"));
        }

        Exam exam = Exam.builder()
                .schoolId(schoolId)
                .academicYear(year)
                .examType(examType)
                .grade(grade)
                .subject(subject)
                .examDate(request.getExamDate())
                .startTime(request.getStartTime())
                .durationMinutes(request.getDurationMinutes())
                .gradingScale(scale)
                .createdBy(createdBy)
                .build();

        Exam saved = examRepository.save(exam);
        log.info("Exam created: {} {} for {} grade {}",
                examType.getName(), subject.getName(),
                year.getLabel(), grade.getName());

        return mapper.toResponse(saved);
    }

    public List<ExamDto.Response> listExams(UUID gradeId, UUID examTypeId) {
        UUID schoolId = TenantContext.getTenantId();
        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        return examRepository
                .findByFilters(schoolId, year.getId(), gradeId, examTypeId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public ExamDto.Response getById(UUID examId) {
        UUID schoolId = TenantContext.getTenantId();
        Exam exam = examRepository.findByIdAndSchoolId(examId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Exam not found"));
        return mapper.toResponse(exam);
    }

    @Transactional
    public ExamDto.Response update(UUID examId, ExamDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();
        Exam exam = examRepository.findByIdAndSchoolId(examId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Exam not found"));

        if (request.getExamDate()        != null)
            exam.setExamDate(request.getExamDate());
        if (request.getStartTime()       != null)
            exam.setStartTime(request.getStartTime());
        if (request.getDurationMinutes() != null)
            exam.setDurationMinutes(request.getDurationMinutes());

        if (request.getGradingScaleId() != null) {
            GradingScale scale = gradingScaleRepository
                    .findByIdAndSchoolId(request.getGradingScaleId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                            "Grading scale not found"));
            exam.setGradingScale(scale);
        }

        return mapper.toResponse(examRepository.save(exam));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID)
            return (UUID) auth.getPrincipal();
        return null;
    }
}