package com.schoolmanagement.module.fee.entity;

import com.schoolmanagement.module.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fee_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_id", nullable = false)
    private StudentFeeLedger ledger;

    @Column(name = "receipt_no", nullable = false, length = 50)
    private String receiptNo;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_mode", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "cheque_no", length = 50)
    private String chequeNo;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "collected_by", nullable = false)
    private UUID collectedBy;

    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum PaymentMode {
        CASH, CHEQUE, UPI, BANK_TRANSFER, DD
    }
}