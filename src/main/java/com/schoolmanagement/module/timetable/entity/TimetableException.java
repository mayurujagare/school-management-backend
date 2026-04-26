package com.schoolmanagement.module.timetable.entity;

import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.entity.Subject;
import com.schoolmanagement.module.staff.entity.Staff;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timetable_exceptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableException {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_timetable_id")
    private Timetable originalTimetable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private TimetablePeriod period;

    @Column(name = "exception_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExceptionType exceptionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_staff_id")
    private Staff replacementStaff;

    private String reason;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ExceptionType {
        SUBSTITUTION,     // different teacher or subject
        CANCELLATION,     // class cancelled
        EXTRA             // additional class not in regular schedule
    }
}