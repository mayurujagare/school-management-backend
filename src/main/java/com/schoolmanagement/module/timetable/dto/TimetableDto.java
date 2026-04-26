package com.schoolmanagement.module.timetable.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolmanagement.module.timetable.entity.TimetableException.ExceptionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TimetableDto {

    // ─────────────────────────────────────────────────────────
    //  PERIODS
    // ─────────────────────────────────────────────────────────

    @Data
    public static class CreatePeriodRequest {

        @NotBlank(message = "Period name is required")
        private String name;

        @NotNull(message = "Start time is required")
        private LocalTime startTime;

        @NotNull(message = "End time is required")
        private LocalTime endTime;

        private Boolean isBreak = false;

        @NotNull(message = "Display order is required")
        private Integer displayOrder;
    }

    @Data
    public static class UpdatePeriodRequest {
        private String    name;
        private LocalTime startTime;
        private LocalTime endTime;
        private Boolean   isBreak;
        private Integer   displayOrder;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PeriodResponse {
        private UUID      id;
        private String    name;
        private LocalTime startTime;
        private LocalTime endTime;
        private Boolean   isBreak;
        private Integer   displayOrder;
        private Integer   durationMinutes;
    }

    // ─────────────────────────────────────────────────────────
    //  TIMETABLE SLOTS
    // ─────────────────────────────────────────────────────────

    @Data
    public static class CreateSlotRequest {

        @NotNull(message = "Section ID is required")
        private UUID sectionId;

        @NotNull(message = "Day of week is required")
        @Min(value = 1, message = "Day must be 1 (Monday) to 6 (Saturday)")
        @Max(value = 6, message = "Day must be 1 (Monday) to 6 (Saturday)")
        private Short dayOfWeek;

        @NotNull(message = "Period ID is required")
        private UUID periodId;

        private UUID subjectId;
        private UUID staffId;

        private LocalDate effectiveFrom;    // null = today
    }

    // Bulk create — set full day schedule
    @Data
    public static class BulkCreateRequest {

        @NotNull(message = "Section ID is required")
        private UUID sectionId;

        @NotNull(message = "Day of week is required")
        @Min(1) @Max(6)
        private Short dayOfWeek;

        @NotEmpty(message = "At least one slot is required")
        @Valid
        private List<SlotEntry> slots;

        private LocalDate effectiveFrom;
    }

    @Data
    public static class SlotEntry {

        @NotNull(message = "Period ID is required")
        private UUID periodId;

        private UUID subjectId;     // null for break periods
        private UUID staffId;       // null for break periods
    }

    // ── Slot response ─────────────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SlotResponse {
        private UUID      id;
        private Short     dayOfWeek;
        private String    dayName;
        private UUID      periodId;
        private String    periodName;
        private LocalTime startTime;
        private LocalTime endTime;
        private Boolean   isBreak;
        private UUID      subjectId;
        private String    subjectName;
        private UUID      staffId;
        private String    staffName;
        private LocalDate effectiveFrom;
        private LocalDate effectiveUntil;
    }

    // ── Full day schedule ─────────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DaySchedule {
        private Short            dayOfWeek;
        private String           dayName;
        private List<SlotResponse> slots;
    }

    // ── Full week schedule ────────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WeekSchedule {
        private UUID              sectionId;
        private String            sectionName;
        private String            gradeName;
        private String            academicYearLabel;
        private List<DaySchedule> days;
    }

    // ── Teacher week schedule ─────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TeacherWeekSchedule {
        private UUID              staffId;
        private String            staffName;
        private List<TeacherSlot> slots;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TeacherSlot {
        private Short     dayOfWeek;
        private String    dayName;
        private String    periodName;
        private LocalTime startTime;
        private LocalTime endTime;
        private String    subjectName;
        private String    gradeName;
        private String    sectionName;
    }

    // ─────────────────────────────────────────────────────────
    //  EXCEPTIONS (Substitutions, Cancellations, Extras)
    // ─────────────────────────────────────────────────────────

    @Data
    public static class CreateExceptionRequest {

        @NotNull(message = "Section ID is required")
        private UUID sectionId;

        @NotNull(message = "Exception date is required")
        private LocalDate exceptionDate;

        @NotNull(message = "Period ID is required")
        private UUID periodId;

        @NotNull(message = "Exception type is required")
        private ExceptionType exceptionType;

        private UUID   subjectId;             // for SUBSTITUTION or EXTRA
        private UUID   replacementStaffId;    // for SUBSTITUTION
        private String reason;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExceptionResponse {
        private UUID          id;
        private UUID          sectionId;
        private String        sectionName;
        private LocalDate     exceptionDate;
        private String        periodName;
        private LocalTime     startTime;
        private LocalTime     endTime;
        private ExceptionType exceptionType;
        private String        subjectName;
        private String        replacementStaffName;
        private String        reason;
        private String        createdByName;
    }

    // ── Effective schedule for a date (regular + exceptions merged) ──
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EffectiveDaySchedule {
        private UUID                    sectionId;
        private String                  sectionName;
        private String                  gradeName;
        private LocalDate               date;
        private String                  dayName;
        private List<EffectiveSlot>     slots;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EffectiveSlot {
        private String        periodName;
        private LocalTime     startTime;
        private LocalTime     endTime;
        private Boolean       isBreak;
        private String        subjectName;
        private String        staffName;
        private Boolean       isException;
        private ExceptionType exceptionType;
        private String        reason;
    }
}