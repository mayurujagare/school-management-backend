package com.schoolmanagement.module.fee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolmanagement.module.fee.entity.FeeDiscount.DiscountType;
import com.schoolmanagement.module.fee.entity.FeePayment.PaymentMode;
import com.schoolmanagement.module.fee.entity.StudentFeeLedger.LedgerStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FeeDto {

    // ── Generate ledger for student ───────────────────────────
    @Data
    public static class GenerateLedgerRequest {

        @NotNull(message = "Student ID is required")
        private UUID studentId;

        private UUID academicYearId;      // null = current year
        private UUID discountId;          // optional discount
    }

    // ── Record payment ────────────────────────────────────────
    @Data
    public static class RecordPaymentRequest {

        @NotNull(message = "Ledger ID is required")
        private UUID ledgerId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amountPaid;

        @NotNull(message = "Payment mode is required")
        private PaymentMode paymentMode;

        @NotNull(message = "Payment date is required")
        private LocalDate paymentDate;

        private String chequeNo;
        private String bankName;
        private String remarks;
    }

    // ── Apply/remove discount on ledger entry ─────────────────
    @Data
    public static class ApplyDiscountRequest {
        @NotNull private UUID ledgerId;
        private UUID discountId;          // null = remove discount
    }

    // ── Waive fee ─────────────────────────────────────────────
    @Data
    public static class WaiveRequest {
        @NotNull private UUID ledgerId;
        private String reason;
    }

    // ── Ledger entry response ─────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LedgerResponse {
        private UUID         id;
        private String       feeCategoryName;
        private String       frequency;
        private LocalDate    dueDate;
        private BigDecimal   amountDue;
        private String       discountName;
        private DiscountType discountType;
        private BigDecimal   discountAmount;
        private BigDecimal   netAmount;
        private BigDecimal   amountPaid;
        private BigDecimal   balanceDue;
        private LedgerStatus status;
        private boolean      overdue;
    }

    // ── Student fee summary ───────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudentFeeSummary {
        private UUID              studentId;
        private String            studentName;
        private String            admissionNo;
        private String            academicYearLabel;
        private BigDecimal        totalDue;
        private BigDecimal        totalPaid;
        private BigDecimal        totalBalance;
        private BigDecimal        totalDiscount;
        private int               pendingCount;
        private int               paidCount;
        private int               overdueCount;
        private List<LedgerResponse> ledger;
    }

    // ── Payment receipt response ──────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReceiptResponse {
        private UUID          id;
        private String        receiptNo;
        private String        studentName;
        private String        admissionNo;
        private String        gradeName;
        private String        sectionName;
        private String        feeCategoryName;
        private BigDecimal    amountPaid;
        private PaymentMode   paymentMode;
        private LocalDate     paymentDate;
        private String        chequeNo;
        private String        bankName;
        private String        collectedByName;
        private String        remarks;
        private LocalDateTime createdAt;
    }

    // ── Daily collection report ───────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DailyCollectionReport {
        private LocalDate             date;
        private BigDecimal            totalCollected;
        private int                   totalTransactions;
        private List<ReceiptResponse> payments;
    }

    // ── Fee discount response ─────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DiscountResponse {
        private UUID         id;
        private String       name;
        private DiscountType discountType;
        private BigDecimal   value;
        private Boolean      isActive;
    }
}