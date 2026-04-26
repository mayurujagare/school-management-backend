package com.schoolmanagement.module.timetable.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.timetable.dto.TimetableDto;
import com.schoolmanagement.module.timetable.service.TimetablePeriodService;
import com.schoolmanagement.module.timetable.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetablePeriodService periodService;
    private final TimetableService       timetableService;

    // ─────────────────────────────────────────────────────────
    //  PERIODS (School-wide time slots)
    // ─────────────────────────────────────────────────────────

    @PostMapping("/periods")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<TimetableDto.PeriodResponse>> createPeriod(
            @Valid @RequestBody TimetableDto.CreatePeriodRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Period created",
                        periodService.create(request)));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<List<TimetableDto.PeriodResponse>>> listPeriods() {

        return ResponseEntity.ok(
                ApiResponse.success(periodService.listAll()));
    }

    @PatchMapping("/periods/{periodId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<TimetableDto.PeriodResponse>> updatePeriod(
            @PathVariable UUID periodId,
            @Valid @RequestBody TimetableDto.UpdatePeriodRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Period updated",
                        periodService.update(periodId, request)));
    }

    // ─────────────────────────────────────────────────────────
    //  TIMETABLE SLOTS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/slots")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<TimetableDto.SlotResponse>> createSlot(
            @Valid @RequestBody TimetableDto.CreateSlotRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Slot created",
                        timetableService.createSlot(request)));
    }

    @PostMapping("/slots/bulk")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<TimetableDto.DaySchedule>> bulkCreateDay(
            @Valid @RequestBody TimetableDto.BulkCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Day schedule created",
                        timetableService.bulkCreateDay(request)));
    }

    // ─────────────────────────────────────────────────────────
    //  VIEW SCHEDULES
    // ─────────────────────────────────────────────────────────

    @GetMapping("/section/{sectionId}/week")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<TimetableDto.WeekSchedule>> getWeekSchedule(
            @PathVariable UUID sectionId) {

        return ResponseEntity.ok(
                ApiResponse.success(timetableService.getWeekSchedule(sectionId)));
    }

    @GetMapping("/section/{sectionId}/date")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<TimetableDto.EffectiveDaySchedule>>
    getEffectiveSchedule(
            @PathVariable UUID sectionId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        timetableService.getEffectiveSchedule(sectionId, date)));
    }

    @GetMapping("/teacher/{staffId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    public ResponseEntity<ApiResponse<TimetableDto.TeacherWeekSchedule>>
    getTeacherSchedule(@PathVariable UUID staffId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        timetableService.getTeacherSchedule(staffId)));
    }

    @GetMapping("/my-schedule")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TimetableDto.TeacherWeekSchedule>>
    getMySchedule(@AuthenticationPrincipal UUID currentUserId) {

        // Teacher's user ID is passed — service resolves to staff ID
        // For now this requires the staff ID lookup
        // TODO: resolve via StaffService.getByUserId
        return ResponseEntity.ok(
                ApiResponse.success("Use /teacher/{staffId} for now"));
    }

    // ─────────────────────────────────────────────────────────
    //  EXCEPTIONS (Substitutions, Cancellations, Extras)
    // ─────────────────────────────────────────────────────────

    @PostMapping("/exceptions")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    public ResponseEntity<ApiResponse<TimetableDto.ExceptionResponse>> createException(
            @Valid @RequestBody TimetableDto.CreateExceptionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Timetable exception created",
                        timetableService.createException(request)));
    }

    @GetMapping("/exceptions/section/{sectionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<List<TimetableDto.ExceptionResponse>>>
    getExceptions(
            @PathVariable UUID sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        timetableService.getExceptions(sectionId, from, to)));
    }
}