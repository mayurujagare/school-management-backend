package com.schoolmanagement.module.fee.entity;

import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_fee_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeeLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount_due", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountDue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private FeeDiscount discount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;      // amountDue - discountAmount

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LedgerStatus status = LedgerStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── helpers ───────────────────────────────────────────────
    public BigDecimal getBalanceDue() {
        return netAmount.subtract(amountPaid);
    }

    public boolean isOverdue() {
        return status != LedgerStatus.PAID &&
               status != LedgerStatus.WAIVED &&
               LocalDate.now().isAfter(dueDate);
    }

    public enum LedgerStatus {
        PENDING, PARTIAL, PAID, WAIVED
    }
}