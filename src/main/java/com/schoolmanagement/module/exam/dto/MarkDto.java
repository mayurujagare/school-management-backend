package com.schoolmanagement.module.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class MarkDto {

    @Data
    public static class BulkEnterRequest {

        @NotNull(message = "Exam ID is required")
        private UUID examId;

        @NotEmpty(message = "Marks records are required")
        @Valid
        private List<StudentMarkEntry> records;
    }

    @Data
    public static class StudentMarkEntry {

        @NotNull
        private UUID studentId;

        private BigDecimal marksObtained;

        @NotNull
        private Boolean isAbsent;

        private String remarks;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID       id;
        private UUID       studentId;
        private String     studentName;
        private String     admissionNo;
        private BigDecimal marksObtained;
        private BigDecimal maxMarks;
        private BigDecimal percentage;
        private String     gradeLabel;
        private Boolean    isAbsent;
        private String     remarks;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExamResultSheet {
        private UUID            examId;
        private String          examTypeName;
        private String          subjectName;
        private String          gradeName;
        private BigDecimal      maxMarks;
        private int             totalStudents;
        private int             totalEntered;
        private int             totalAbsent;
        private BigDecimal      averageMarks;
        private BigDecimal      highestMarks;
        private BigDecimal      lowestMarks;
        private List<Response>  marks;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudentReportCard {
        private UUID         studentId;
        private String       studentName;
        private String       admissionNo;
        private String       gradeName;
        private String       sectionName;
        private String       academicYearLabel;
        private List<SubjectResult> subjects;
        private BigDecimal   overallPercentage;
        private String       overallGrade;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubjectResult {
        private String       subjectName;
        private List<ExamMark> exams;
        private BigDecimal   totalMarks;
        private BigDecimal   maxMarks;
        private BigDecimal   percentage;
        private String       gradeLabel;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExamMark {
        private String     examTypeName;
        private BigDecimal marksObtained;
        private BigDecimal maxMarks;
        private Boolean    isAbsent;
    }
}