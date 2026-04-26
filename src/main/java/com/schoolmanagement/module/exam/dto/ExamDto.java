package com.schoolmanagement.module.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ExamDto {

    @Data
    public static class CreateRequest {

        @NotNull(message = "Exam type is required")
        private UUID examTypeId;

        @NotNull(message = "Grade is required")
        private UUID gradeId;

        @NotNull(message = "Subject is required")
        private UUID subjectId;

        private LocalDate  examDate;
        private LocalTime  startTime;
        private Integer    durationMinutes;
        private UUID       gradingScaleId;
        private UUID       academicYearId;
    }

    @Data
    public static class UpdateRequest {
        private LocalDate  examDate;
        private LocalTime  startTime;
        private Integer    durationMinutes;
        private UUID       gradingScaleId;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID       id;
        private UUID       examTypeId;
        private String     examTypeName;
        private BigDecimal maxMarks;
        private UUID       gradeId;
        private String     gradeName;
        private UUID       subjectId;
        private String     subjectName;
        private LocalDate  examDate;
        private LocalTime  startTime;
        private Integer    durationMinutes;
        private String     academicYearLabel;
    }
}