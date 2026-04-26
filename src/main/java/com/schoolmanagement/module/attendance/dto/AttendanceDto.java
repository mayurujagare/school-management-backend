package com.schoolmanagement.module.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolmanagement.module.attendance.entity.Attendance.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AttendanceDto {

    // ── Mark attendance for full section ──────────────────────
    @Data
    public static class BulkMarkRequest {

        @NotNull(message = "Section ID is required")
        private UUID sectionId;

        @NotNull(message = "Date is required")
        private LocalDate date;

        @NotEmpty(message = "Attendance records are required")
        @Valid
        private List<StudentAttendance> records;
    }

    // ── Single student attendance entry ───────────────────────
    @Data
    public static class StudentAttendance {

        @NotNull(message = "Student ID is required")
        private UUID studentId;

        @NotNull(message = "Status is required")
        private AttendanceStatus status;

        private String remarks;
    }

    // ── Update single student attendance ──────────────────────
    @Data
    public static class UpdateRequest {

        @NotNull(message = "Status is required")
        private AttendanceStatus status;

        private String remarks;
    }

    // ── Attendance response per student ───────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID             id;
        private UUID             studentId;
        private String           studentName;
        private String           admissionNo;
        private LocalDate        date;
        private AttendanceStatus status;
        private String           remarks;
        private String           markedByName;
    }

    // ── Section attendance for a date (full list) ─────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SectionAttendanceResponse {
        private UUID              sectionId;
        private String            sectionName;
        private String            gradeName;
        private LocalDate         date;
        private boolean           alreadyMarked;
        private int               totalStudents;
        private int               presentCount;
        private int               absentCount;
        private int               lateCount;
        private int               leaveCount;
        private List<Response>    records;
    }

    // ── Student attendance summary (for reports) ──────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudentSummary {
        private UUID                       studentId;
        private String                     studentName;
        private String                     admissionNo;
        private int                        totalWorkingDays;
        private int                        presentDays;
        private int                        absentDays;
        private int                        lateDays;
        private int                        leaveDays;
        private double                     attendancePercentage;
        private Map<String, Integer>       monthlyBreakdown;
    }

    // ── Date range filter ─────────────────────────────────────
    @Data
    public static class DateRangeRequest {
        @NotNull private LocalDate from;
        @NotNull private LocalDate to;
    }
}