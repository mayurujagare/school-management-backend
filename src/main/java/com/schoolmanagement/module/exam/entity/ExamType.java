package com.schoolmanagement.module.exam.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_marks", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxMarks;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightage;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}