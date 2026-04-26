package com.schoolmanagement.module.timetable.entity;

import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.entity.Subject;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.staff.entity.Staff;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "timetable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timetable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;            // 1=Monday ... 6=Saturday

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private TimetablePeriod period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;   // NULL = currently active

    // ── helper ────────────────────────────────────────────────
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        boolean started = !today.isBefore(effectiveFrom);
        boolean notEnded = effectiveUntil == null || !today.isAfter(effectiveUntil);
        return started && notEnded;
    }

    public String getDayName() {
        return switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            default -> "Unknown";
        };
    }
}