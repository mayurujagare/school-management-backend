package com.schoolmanagement.module.timetable.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.entity.Subject;
import com.schoolmanagement.module.academic.repository.SectionRepository;
import com.schoolmanagement.module.academic.repository.SubjectRepository;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.service.AcademicYearService;
import com.schoolmanagement.module.staff.entity.Staff;
import com.schoolmanagement.module.staff.repository.StaffRepository;
import com.schoolmanagement.module.timetable.dto.TimetableDto;
import com.schoolmanagement.module.timetable.entity.Timetable;
import com.schoolmanagement.module.timetable.entity.TimetableException;
import com.schoolmanagement.module.timetable.entity.TimetableException.ExceptionType;
import com.schoolmanagement.module.timetable.entity.TimetablePeriod;
import com.schoolmanagement.module.timetable.mapper.TimetableMapper;
import com.schoolmanagement.module.timetable.repository.TimetableExceptionRepository;
import com.schoolmanagement.module.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository          timetableRepository;
    private final TimetableExceptionRepository exceptionRepository;
    private final SectionRepository            sectionRepository;
    private final SubjectRepository            subjectRepository;
    private final StaffRepository              staffRepository;
    private final TimetablePeriodService       periodService;
    private final AcademicYearService          academicYearService;
    private final TimetableMapper              mapper;

    // ── Create single slot ────────────────────────────────────
    @Transactional
    public TimetableDto.SlotResponse createSlot(TimetableDto.CreateSlotRequest request) {
        UUID schoolId = TenantContext.getTenantId();
        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);
        LocalDate effectiveFrom = request.getEffectiveFrom() != null
                ? request.getEffectiveFrom() : LocalDate.now();

        Section section = sectionRepository
                .findByIdAndSchoolId(request.getSectionId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        TimetablePeriod period = periodService.validateAndGet(request.getPeriodId(), schoolId);

        // Check if slot already exists
        if (timetableRepository.existsActiveSlot(
                section.getId(), request.getDayOfWeek(),
                period.getId(), year.getId(), effectiveFrom)) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "This time slot already has a class assigned");
        }

        Subject subject = null;
        Staff staff = null;

        if (request.getSubjectId() != null) {
            subject = subjectRepository.findByIdAndSchoolId(request.getSubjectId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Subject not found"));
        }

        if (request.getStaffId() != null) {
            staff = staffRepository.findByIdAndSchoolId(request.getStaffId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Staff not found"));

            // Check teacher conflict
            if (timetableRepository.isTeacherBusyAtSlot(
                    staff.getId(), request.getDayOfWeek(), period.getId(),
                    year.getId(), effectiveFrom, section.getId())) {
                throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                        "Teacher " + staff.getFullName() +
                                " is already assigned at this time in another section");
            }
        }

        Timetable timetable = Timetable.builder()
                .schoolId(schoolId)
                .section(section)
                .academicYear(year)
                .dayOfWeek(request.getDayOfWeek())
                .period(period)
                .subject(subject)
                .staff(staff)
                .effectiveFrom(effectiveFrom)
                .build();

        return mapper.toResponse(timetableRepository.save(timetable));
    }

    // ── Bulk create day schedule ──────────────────────────────
    @Transactional
    public TimetableDto.DaySchedule bulkCreateDay(TimetableDto.BulkCreateRequest request) {
        UUID schoolId = TenantContext.getTenantId();
        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);
        LocalDate effectiveFrom = request.getEffectiveFrom() != null
                ? request.getEffectiveFrom() : LocalDate.now();

        Section section = sectionRepository
                .findByIdAndSchoolId(request.getSectionId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        // Deactivate existing slots for this day
        List<Timetable> existingSlots = timetableRepository
                .findActiveSlot(section.getId(), request.getDayOfWeek(),
                        null, year.getId(), effectiveFrom);

        // Actually, findActiveSlot requires periodId — deactivate per period
        for (TimetableDto.SlotEntry entry : request.getSlots()) {
            List<Timetable> existing = timetableRepository
                    .findActiveSlot(section.getId(), request.getDayOfWeek(),
                            entry.getPeriodId(), year.getId(), effectiveFrom);
            for (Timetable t : existing) {
                t.setEffectiveUntil(effectiveFrom.minusDays(1));
                timetableRepository.save(t);
            }
        }

        // Create new slots
        List<TimetableDto.SlotResponse> createdSlots = new ArrayList<>();

        for (TimetableDto.SlotEntry entry : request.getSlots()) {
            TimetablePeriod period = periodService.validateAndGet(entry.getPeriodId(), schoolId);

            Subject subject = null;
            Staff staff = null;

            if (entry.getSubjectId() != null) {
                subject = subjectRepository.findByIdAndSchoolId(entry.getSubjectId(), schoolId)
                        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Subject not found"));
            }

            if (entry.getStaffId() != null) {
                staff = staffRepository.findByIdAndSchoolId(entry.getStaffId(), schoolId)
                        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Staff not found"));
            }

            Timetable timetable = Timetable.builder()
                    .schoolId(schoolId)
                    .section(section)
                    .academicYear(year)
                    .dayOfWeek(request.getDayOfWeek())
                    .period(period)
                    .subject(subject)
                    .staff(staff)
                    .effectiveFrom(effectiveFrom)
                    .build();

            createdSlots.add(mapper.toResponse(timetableRepository.save(timetable)));
        }

        String dayName = createdSlots.isEmpty() ? "Unknown"
                : createdSlots.get(0).getDayName();

        log.info("Bulk timetable set for section [{}] day [{}] — {} slots",
                section.getName(), dayName, createdSlots.size());

        return TimetableDto.DaySchedule.builder()
                .dayOfWeek(request.getDayOfWeek())
                .dayName(dayName)
                .slots(createdSlots)
                .build();
    }

    // ── Get weekly schedule for section ───────────────────────
    public TimetableDto.WeekSchedule getWeekSchedule(UUID sectionId) {
        UUID schoolId = TenantContext.getTenantId();

        Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        List<Timetable> allSlots = timetableRepository
                .findActiveBySectionAndYear(sectionId, year.getId(), LocalDate.now());

        // Group by day
        Map<Short, List<TimetableDto.SlotResponse>> byDay = allSlots.stream()
                .map(mapper::toResponse)
                .collect(Collectors.groupingBy(
                        TimetableDto.SlotResponse::getDayOfWeek,
                        TreeMap::new,
                        Collectors.toList()));

        List<TimetableDto.DaySchedule> days = new ArrayList<>();
        for (Map.Entry<Short, List<TimetableDto.SlotResponse>> e : byDay.entrySet()) {
            days.add(TimetableDto.DaySchedule.builder()
                    .dayOfWeek(e.getKey())
                    .dayName(e.getValue().get(0).getDayName())
                    .slots(e.getValue())
                    .build());
        }

        return TimetableDto.WeekSchedule.builder()
                .sectionId(section.getId())
                .sectionName(section.getName())
                .gradeName(section.getGrade().getName())
                .academicYearLabel(year.getLabel())
                .days(days)
                .build();
    }

    // ── Get effective schedule for a specific date ─────────────
    public TimetableDto.EffectiveDaySchedule getEffectiveSchedule(
            UUID sectionId, LocalDate date) {

        UUID schoolId = TenantContext.getTenantId();

        Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // Get day of week (1=Mon...6=Sat)
        short dayOfWeek = (short) targetDate.getDayOfWeek().getValue();

        if (dayOfWeek == 7) { // Sunday
            return TimetableDto.EffectiveDaySchedule.builder()
                    .sectionId(section.getId())
                    .sectionName(section.getName())
                    .gradeName(section.getGrade().getName())
                    .date(targetDate)
                    .dayName("Sunday")
                    .slots(List.of())
                    .build();
        }

        // Regular timetable for this day
        List<Timetable> regular = timetableRepository
                .findBySectionDayAndYear(sectionId, dayOfWeek,
                        year.getId(), targetDate);

        // Exceptions for this date
        List<TimetableException> exceptions = exceptionRepository
                .findBySectionAndDate(sectionId, targetDate);

        // Build exception lookup by period ID
        Map<UUID, TimetableException> exceptionMap = exceptions.stream()
                .collect(Collectors.toMap(
                        te -> te.getPeriod().getId(),
                        te -> te,
                        (a, b) -> b));

        List<TimetableDto.EffectiveSlot> effectiveSlots = new ArrayList<>();

        for (Timetable t : regular) {
            UUID periodId = t.getPeriod().getId();
            TimetableException ex = exceptionMap.remove(periodId);

            if (ex != null) {
                if (ex.getExceptionType() == ExceptionType.CANCELLATION) {
                    // Show as cancelled
                    TimetableDto.EffectiveSlot slot = mapper.toEffectiveSlot(ex);
                    effectiveSlots.add(slot);
                } else {
                    // Show exception (substitution)
                    effectiveSlots.add(mapper.toEffectiveSlot(ex));
                }
            } else {
                // Regular slot
                effectiveSlots.add(mapper.toEffectiveSlot(t));
            }
        }

        // Add EXTRA classes that don't replace regular slots
        for (TimetableException extra : exceptionMap.values()) {
            if (extra.getExceptionType() == ExceptionType.EXTRA) {
                effectiveSlots.add(mapper.toEffectiveSlot(extra));
            }
        }

        // Sort by start time
        effectiveSlots.sort(Comparator.comparing(TimetableDto.EffectiveSlot::getStartTime));

        return TimetableDto.EffectiveDaySchedule.builder()
                .sectionId(section.getId())
                .sectionName(section.getName())
                .gradeName(section.getGrade().getName())
                .date(targetDate)
                .dayName(targetDate.getDayOfWeek().toString())
                .slots(effectiveSlots)
                .build();
    }

    // ── Get teacher's weekly schedule ─────────────────────────
    public TimetableDto.TeacherWeekSchedule getTeacherSchedule(UUID staffId) {
        UUID schoolId = TenantContext.getTenantId();
        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        Staff staff = staffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Staff not found"));

        List<Timetable> slots = timetableRepository
                .findByStaffAndYear(staffId, year.getId(), LocalDate.now());

        return TimetableDto.TeacherWeekSchedule.builder()
                .staffId(staff.getId())
                .staffName(staff.getFullName())
                .slots(slots.stream()
                        .map(mapper::toTeacherSlot)
                        .collect(Collectors.toList()))
                .build();
    }

    // ── Create exception ──────────────────────────────────────
    @Transactional
    public TimetableDto.ExceptionResponse createException(
            TimetableDto.CreateExceptionRequest request) {

        UUID schoolId  = TenantContext.getTenantId();
        UUID createdBy = getCurrentUserId();

        Section section = sectionRepository
                .findByIdAndSchoolId(request.getSectionId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        TimetablePeriod period = periodService.validateAndGet(request.getPeriodId(), schoolId);

        // Prevent duplicate exception
        if (exceptionRepository.existsBySection_IdAndExceptionDateAndPeriod_Id(
                section.getId(), request.getExceptionDate(), period.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "An exception already exists for this slot on this date");
        }

        // Validate date is not in the past
        if (request.getExceptionDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot create exception for a past date");
        }

        Subject subject = null;
        Staff replacementStaff = null;

        if (request.getSubjectId() != null) {
            subject = subjectRepository.findByIdAndSchoolId(request.getSubjectId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Subject not found"));
        }

        if (request.getReplacementStaffId() != null) {
            replacementStaff = staffRepository
                    .findByIdAndSchoolId(request.getReplacementStaffId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Staff not found"));
        }

        TimetableException exception = TimetableException.builder()
                .schoolId(schoolId)
                .section(section)
                .exceptionDate(request.getExceptionDate())
                .period(period)
                .exceptionType(request.getExceptionType())
                .subject(subject)
                .replacementStaff(replacementStaff)
                .reason(request.getReason())
                .createdBy(createdBy)
                .build();

        TimetableException saved = exceptionRepository.save(exception);

        log.info("Timetable exception created: {} for section [{}] date [{}] period [{}]",
                saved.getExceptionType(), section.getName(),
                request.getExceptionDate(), period.getName());

        return mapper.toResponse(saved);
    }

    // ── Get exceptions for section in date range ──────────────
    public List<TimetableDto.ExceptionResponse> getExceptions(
            UUID sectionId, LocalDate from, LocalDate to) {

        UUID schoolId = TenantContext.getTenantId();

        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        return exceptionRepository
                .findBySectionAndDateRange(sectionId, from, to)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID)
            return (UUID) auth.getPrincipal();
        return null;
    }
}