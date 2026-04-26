package com.schoolmanagement.module.exam.entity;

import com.schoolmanagement.module.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_marks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "marks_obtained", precision = 6, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "is_absent", nullable = false)
    @Builder.Default
    private Boolean isAbsent = false;

    private String remarks;

    @Column(name = "entered_by")
    private UUID enteredBy;

    @CreationTimestamp
    @Column(name = "entered_at", updatable = false)
    private LocalDateTime enteredAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}