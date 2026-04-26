package com.schoolmanagement.module.fee.entity;

import com.schoolmanagement.module.academic.entity.Grade;
import com.schoolmanagement.module.school.entity.AcademicYear;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fee_structures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_category_id", nullable = false)
    private FeeCategory feeCategory;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FeeFrequency frequency;

    @Column(name = "due_day")
    private Integer dueDay;            // day of month due (for MONTHLY)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum FeeFrequency {
        MONTHLY, QUARTERLY, ANNUAL, ONE_TIME
    }
}