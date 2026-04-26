package com.schoolmanagement.module.exam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grading_scale_levels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingScaleLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_scale_id", nullable = false)
    private GradingScale gradingScale;

    @Column(name = "grade_label", nullable = false, length = 10)
    private String gradeLabel;

    @Column(name = "min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercentage;

    @Column(name = "grade_point", precision = 4, scale = 2)
    private BigDecimal gradePoint;

    @Column(length = 100)
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}