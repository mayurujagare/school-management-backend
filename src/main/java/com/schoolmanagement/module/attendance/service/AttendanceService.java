package com.schoolmanagement.module.attendance.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.repository.SectionRepository;
import com.schoolmanagement.module.attendance.dto.AttendanceDto;
import com.schoolmanagement.module.attendance.entity.Attendance;
import com.schoolmanagement.module.attendance.entity.Attendance.AttendanceStatus;
import com.schoolmanagement.module.attendance.mapper.AttendanceMapper;
import com.schoolmanagement.module.attendance.repository.AttendanceRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository       attendanceRepository;
    private final StudentRepository          studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SectionRepository          sectionRepository;
    private final AcademicYearService        academicYearService;
    private final AttendanceMapper           mapper;

    // ── Mark bulk attendance for section ──────────────────────
    @Transactional
    public AttendanceDto.SectionAttendanceResponse markBulk(
            AttendanceDto.BulkMarkRequest request) {

        UUID schoolId  = TenantContext.getTenantId();
        UUID markedBy  = getCurrentUserId();

        // Validate date is not in future
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot mark attendance for a future date");
        }

        // Validate date is not a Sunday
        if (request.getDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot mark attendance on Sunday");
        }

        Section section = sectionRepository
                .findByIdAndSchoolId(request.getSectionId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        // Validate date falls within academic year
        if (request.getDate().isBefore(year.getStartDate()) ||
                request.getDate().isAfter(year.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Date is outside the current academic year");
        }

        List<Attendance> saved = new ArrayList<>();

        for (AttendanceDto.StudentAttendance record : request.getRecords()) {

            Student student = studentRepository
                    .findByIdAndSchoolId(record.getStudentId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND,
                            "Student not found: " + record.getStudentId()));

            // Upsert — update if already exists for this date
            Attendance attendance = attendanceRepository
                    .findByStudent_IdAndDate(student.getId(), request.getDate())
                    .orElse(Attendance.builder()
                            .schoolId(schoolId)
                            .student(student)
                            .section(section)
                            .academicYear(year)
                            .date(request.getDate())
                            .build());

            attendance.setStatus(record.getStatus());
            attendance.setRemarks(record.getRemarks());
            attendance.setMarkedBy(markedBy);

            saved.add(attendanceRepository.save(attendance));
        }

        log.info("Attendance marked for section [{}] date [{}] — {} records",
                section.getName(), request.getDate(), saved.size());

        return buildSectionResponse(section, request.getDate(), saved);
    }

    // ── Get section attendance for a date ─────────────────────
    public AttendanceDto.SectionAttendanceResponse getSectionAttendance(
            UUID sectionId, LocalDate date) {

        UUID schoolId = TenantContext.getTenantId();

        Section section = sectionRepository
                .findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<Attendance> records = attendanceRepository
                .findBySectionAndDate(sectionId, targetDate);

        return buildSectionResponse(section, targetDate, records);
    }

    // ── Get attendance sheet for section (date range) ─────────
    public List<AttendanceDto.Response> getSectionAttendanceRange(
            UUID sectionId, LocalDate from, LocalDate to) {

        UUID schoolId = TenantContext.getTenantId();

        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        validateDateRange(from, to);

        return attendanceRepository
                .findBySectionAndDateRange(sectionId, from, to)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get student attendance (date range) ───────────────────
    public List<AttendanceDto.Response> getStudentAttendance(
            UUID studentId, LocalDate from, LocalDate to) {

        UUID schoolId = TenantContext.getTenantId();

        studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        validateDateRange(from, to);

        return attendanceRepository
                .findByStudentAndDateRange(studentId, from, to)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get student attendance summary ────────────────────────
    public AttendanceDto.StudentSummary getStudentSummary(
            UUID studentId, UUID academicYearId) {

        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        UUID yearId = academicYearId != null
                ? academicYearId
                : academicYearService.getCurrentYearEntity(schoolId).getId();

        // Get status counts
        List<Object[]> counts = attendanceRepository
                .countByStatusForStudent(studentId, yearId);

        Map<AttendanceStatus, Integer> statusMap = new HashMap<>();
        int total = 0;

        for (Object[] row : counts) {
            AttendanceStatus status = (AttendanceStatus) row[0];
            int count = ((Long) row[1]).intValue();
            statusMap.put(status, count);
            if (status != AttendanceStatus.HOLIDAY) {
                total += count;
            }
        }

        int present  = statusMap.getOrDefault(AttendanceStatus.PRESENT,  0);
        int absent   = statusMap.getOrDefault(AttendanceStatus.ABSENT,   0);
        int late     = statusMap.getOrDefault(AttendanceStatus.LATE,     0);
        int leave    = statusMap.getOrDefault(AttendanceStatus.LEAVE,    0);

        double percentage = total > 0
                ? Math.round(((present + late) * 100.0 / total) * 100.0) / 100.0
                : 0.0;

        return AttendanceDto.StudentSummary.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .admissionNo(student.getAdmissionNo())
                .totalWorkingDays(total)
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .leaveDays(leave)
                .attendancePercentage(percentage)
                .build();
    }

    // ── Update single student attendance ──────────────────────
    @Transactional
    public AttendanceDto.Response updateAttendance(UUID attendanceId,
                                                    AttendanceDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Attendance attendance = attendanceRepository
                .findById(attendanceId)
                .filter(a -> a.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Attendance record not found"));

        attendance.setStatus(request.getStatus());
        if (request.getRemarks() != null)
            attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(getCurrentUserId());

        return mapper.toResponse(attendanceRepository.save(attendance));
    }

    // ── Parent: get own child's attendance ────────────────────
    public List<AttendanceDto.Response> getMyChildAttendance(
            UUID studentId, LocalDate from, LocalDate to) {

        UUID schoolId = TenantContext.getTenantId();
        UUID userId   = getCurrentUserId();

        // Verify this student is linked to the logged-in parent
        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        validateDateRange(from, to);

        return attendanceRepository
                .findByStudentAndDateRange(studentId, from, to)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Private: build section attendance response ─────────────
    private AttendanceDto.SectionAttendanceResponse buildSectionResponse(
            Section section,
            LocalDate date,
            List<Attendance> records) {

        List<AttendanceDto.Response> responseList = records.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        long presentCount = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absentCount  = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long lateCount    = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long leaveCount   = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();

        return AttendanceDto.SectionAttendanceResponse.builder()
                .sectionId(section.getId())
                .sectionName(section.getName())
                .gradeName(section.getGrade().getName())
                .date(date)
                .alreadyMarked(!records.isEmpty())
                .totalStudents(records.size())
                .presentCount((int) presentCount)
                .absentCount((int) absentCount)
                .lateCount((int) lateCount)
                .leaveCount((int) leaveCount)
                .records(responseList)
                .build();
    }

    // ── Private: validate date range ──────────────────────────
    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "From date must be before to date");
        }
        if (from.plusDays(90).isBefore(to)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Date range cannot exceed 90 days");
        }
    }

    // ── Private: get current user from security context ────────
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID) {
            return (UUID) auth.getPrincipal();
        }
        return null;
    }
}