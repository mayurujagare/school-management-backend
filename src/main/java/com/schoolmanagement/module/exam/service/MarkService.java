package com.schoolmanagement.module.exam.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.exam.dto.MarkDto;
import com.schoolmanagement.module.exam.entity.Exam;
import com.schoolmanagement.module.exam.entity.GradingScaleLevel;
import com.schoolmanagement.module.exam.entity.StudentMark;
import com.schoolmanagement.module.exam.mapper.ExamMapper;
import com.schoolmanagement.module.exam.repository.ExamRepository;
import com.schoolmanagement.module.exam.repository.StudentMarkRepository;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.service.AcademicYearService;
import com.schoolmanagement.module.student.entity.Student;
import com.schoolmanagement.module.student.entity.StudentEnrollment;
import com.schoolmanagement.module.student.repository.StudentEnrollmentRepository;
import com.schoolmanagement.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkService {

    private final StudentMarkRepository       markRepository;
    private final ExamRepository              examRepository;
    private final StudentRepository           studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final GradingScaleService         gradingScaleService;
    private final AcademicYearService         academicYearService;
    private final ExamMapper                  mapper;

    @Transactional
    public MarkDto.ExamResultSheet bulkEnter(MarkDto.BulkEnterRequest request) {
        UUID schoolId  = TenantContext.getTenantId();
        UUID enteredBy = getCurrentUserId();

        Exam exam = examRepository
                .findByIdAndSchoolId(request.getExamId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Exam not found"));

        BigDecimal maxMarks = exam.getExamType().getMaxMarks();

        for (MarkDto.StudentMarkEntry entry : request.getRecords()) {

            Student student = studentRepository
                    .findByIdAndSchoolId(entry.getStudentId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND,
                            "Student not found: " + entry.getStudentId()));

            if (!Boolean.TRUE.equals(entry.getIsAbsent()) &&
                    entry.getMarksObtained() != null &&
                    entry.getMarksObtained().compareTo(maxMarks) > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Marks cannot exceed " + maxMarks + " for " +
                        student.getFullName());
            }

            StudentMark mark = markRepository
                    .findByExam_IdAndStudent_Id(exam.getId(), student.getId())
                    .orElse(StudentMark.builder()
                            .exam(exam)
                            .student(student)
                            .build());

            mark.setMarksObtained(
                    Boolean.TRUE.equals(entry.getIsAbsent())
                            ? null : entry.getMarksObtained());
            mark.setIsAbsent(entry.getIsAbsent());
            mark.setRemarks(entry.getRemarks());
            mark.setEnteredBy(enteredBy);

            markRepository.save(mark);
        }

        log.info("Marks entered for exam [{}] - {} records",
                exam.getId(), request.getRecords().size());

        return getExamResultSheet(exam.getId());
    }

    public MarkDto.ExamResultSheet getExamResultSheet(UUID examId) {
        UUID schoolId = TenantContext.getTenantId();

        Exam exam = examRepository
                .findByIdAndSchoolId(examId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Exam not found"));

        BigDecimal maxMarks = exam.getExamType().getMaxMarks();

        List<GradingScaleLevel> levels = exam.getGradingScale() != null
                ? gradingScaleService.getLevels(exam.getGradingScale().getId())
                : List.of();

        List<StudentMark> marks = markRepository.findByExamId(examId);

        List<MarkDto.Response> responses = marks.stream()
                .map(m -> mapper.toResponse(m, maxMarks, levels))
                .collect(Collectors.toList());

        List<BigDecimal> validMarks = marks.stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsAbsent()))
                .map(StudentMark::getMarksObtained)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal avg = validMarks.isEmpty()
                ? BigDecimal.ZERO
                : validMarks.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(validMarks.size()),
                            2, RoundingMode.HALF_UP);

        BigDecimal high = validMarks.stream()
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal low  = validMarks.stream()
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        long absent = marks.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsAbsent())).count();

        return MarkDto.ExamResultSheet.builder()
                .examId(exam.getId())
                .examTypeName(exam.getExamType().getName())
                .subjectName(exam.getSubject().getName())
                .gradeName(exam.getGrade().getName())
                .maxMarks(maxMarks)
                .totalStudents(marks.size())
                .totalEntered(marks.size() - (int) absent)
                .totalAbsent((int) absent)
                .averageMarks(avg)
                .highestMarks(high)
                .lowestMarks(low)
                .marks(responses)
                .build();
    }

    public MarkDto.StudentReportCard getReportCard(UUID studentId,
                                                    UUID academicYearId) {
        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        StudentEnrollment enrollment = enrollmentRepository
                .findByStudent_IdAndAcademicYear_Id(student.getId(), year.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Student enrollment not found for this year"));

        List<StudentMark> marks = markRepository
                .findByStudentAndYear(student.getId(), year.getId());

        Map<String, List<StudentMark>> bySubject = marks.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getExam().getSubject().getName(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<MarkDto.SubjectResult> subjectResults = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal grandMax   = BigDecimal.ZERO;

        for (Map.Entry<String, List<StudentMark>> e : bySubject.entrySet()) {
            String subject = e.getKey();
            List<StudentMark> subjectMarks = e.getValue();

            BigDecimal totalMarks = BigDecimal.ZERO;
            BigDecimal maxTotal   = BigDecimal.ZERO;
            List<MarkDto.ExamMark> examList = new ArrayList<>();

            for (StudentMark m : subjectMarks) {
                BigDecimal max = m.getExam().getExamType().getMaxMarks();
                if (!Boolean.TRUE.equals(m.getIsAbsent()) &&
                        m.getMarksObtained() != null) {
                    totalMarks = totalMarks.add(m.getMarksObtained());
                }
                maxTotal = maxTotal.add(max);

                examList.add(MarkDto.ExamMark.builder()
                        .examTypeName(m.getExam().getExamType().getName())
                        .marksObtained(m.getMarksObtained())
                        .maxMarks(max)
                        .isAbsent(m.getIsAbsent())
                        .build());
            }

            BigDecimal pct = maxTotal.compareTo(BigDecimal.ZERO) > 0
                    ? totalMarks.multiply(BigDecimal.valueOf(100))
                            .divide(maxTotal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            subjectResults.add(MarkDto.SubjectResult.builder()
                    .subjectName(subject)
                    .exams(examList)
                    .totalMarks(totalMarks)
                    .maxMarks(maxTotal)
                    .percentage(pct)
                    .build());

            grandTotal = grandTotal.add(totalMarks);
            grandMax   = grandMax.add(maxTotal);
        }

        BigDecimal overallPct = grandMax.compareTo(BigDecimal.ZERO) > 0
                ? grandTotal.multiply(BigDecimal.valueOf(100))
                        .divide(grandMax, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return MarkDto.StudentReportCard.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .admissionNo(student.getAdmissionNo())
                .gradeName(enrollment.getSection().getGrade().getName())
                .sectionName(enrollment.getSection().getName())
                .academicYearLabel(year.getLabel())
                .subjects(subjectResults)
                .overallPercentage(overallPct)
                .build();
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID)
            return (UUID) auth.getPrincipal();
        return null;
    }
}