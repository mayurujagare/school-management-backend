package com.schoolmanagement.module.exam.mapper;

import com.schoolmanagement.module.exam.dto.ExamDto;
import com.schoolmanagement.module.exam.dto.ExamTypeDto;
import com.schoolmanagement.module.exam.dto.GradingScaleDto;
import com.schoolmanagement.module.exam.dto.MarkDto;
import com.schoolmanagement.module.exam.entity.Exam;
import com.schoolmanagement.module.exam.entity.ExamType;
import com.schoolmanagement.module.exam.entity.GradingScale;
import com.schoolmanagement.module.exam.entity.GradingScaleLevel;
import com.schoolmanagement.module.exam.entity.StudentMark;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExamMapper {

    public ExamTypeDto.Response toResponse(ExamType et) {
        return ExamTypeDto.Response.builder()
                .id(et.getId())
                .name(et.getName())
                .maxMarks(et.getMaxMarks())
                .weightage(et.getWeightage())
                .isActive(et.getIsActive())
                .build();
    }

    public ExamDto.Response toResponse(Exam e) {
        return ExamDto.Response.builder()
                .id(e.getId())
                .examTypeId(e.getExamType().getId())
                .examTypeName(e.getExamType().getName())
                .maxMarks(e.getExamType().getMaxMarks())
                .gradeId(e.getGrade().getId())
                .gradeName(e.getGrade().getName())
                .subjectId(e.getSubject().getId())
                .subjectName(e.getSubject().getName())
                .examDate(e.getExamDate())
                .startTime(e.getStartTime())
                .durationMinutes(e.getDurationMinutes())
                .academicYearLabel(e.getAcademicYear().getLabel())
                .build();
    }

    public GradingScaleDto.Response toResponse(GradingScale gs,
                                                List<GradingScaleLevel> levels) {
        return GradingScaleDto.Response.builder()
                .id(gs.getId())
                .name(gs.getName())
                .isActive(gs.getIsActive())
                .levels(levels.stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public GradingScaleDto.LevelResponse toResponse(GradingScaleLevel l) {
        return GradingScaleDto.LevelResponse.builder()
                .id(l.getId())
                .gradeLabel(l.getGradeLabel())
                .minPercentage(l.getMinPercentage())
                .maxPercentage(l.getMaxPercentage())
                .gradePoint(l.getGradePoint())
                .description(l.getDescription())
                .displayOrder(l.getDisplayOrder())
                .build();
    }

    public MarkDto.Response toResponse(StudentMark m,
                                         BigDecimal maxMarks,
                                         List<GradingScaleLevel> levels) {
        BigDecimal percentage = null;
        String gradeLabel = null;

        if (!Boolean.TRUE.equals(m.getIsAbsent()) &&
                m.getMarksObtained() != null &&
                maxMarks != null && maxMarks.compareTo(BigDecimal.ZERO) > 0) {

            percentage = m.getMarksObtained()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(maxMarks, 2, RoundingMode.HALF_UP);

            gradeLabel = findGradeLabel(percentage, levels);
        }

        return MarkDto.Response.builder()
                .id(m.getId())
                .studentId(m.getStudent().getId())
                .studentName(m.getStudent().getFullName())
                .admissionNo(m.getStudent().getAdmissionNo())
                .marksObtained(m.getMarksObtained())
                .maxMarks(maxMarks)
                .percentage(percentage)
                .gradeLabel(gradeLabel)
                .isAbsent(m.getIsAbsent())
                .remarks(m.getRemarks())
                .build();
    }

    private String findGradeLabel(BigDecimal percentage,
                                   List<GradingScaleLevel> levels) {
        if (levels == null || levels.isEmpty()) return null;

        return levels.stream()
                .filter(l -> percentage.compareTo(l.getMinPercentage()) >= 0 &&
                             percentage.compareTo(l.getMaxPercentage()) <= 0)
                .map(GradingScaleLevel::getGradeLabel)
                .findFirst()
                .orElse(null);
    }
}