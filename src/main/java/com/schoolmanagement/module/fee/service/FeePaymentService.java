package com.schoolmanagement.module.fee.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.fee.dto.FeeDto;
import com.schoolmanagement.module.fee.entity.FeePayment;
import com.schoolmanagement.module.fee.entity.StudentFeeLedger;
import com.schoolmanagement.module.fee.entity.StudentFeeLedger.LedgerStatus;
import com.schoolmanagement.module.fee.mapper.FeeMapper;
import com.schoolmanagement.module.fee.repository.FeePaymentRepository;
import com.schoolmanagement.module.fee.repository.StudentFeeLedgerRepository;
import com.schoolmanagement.module.fee.util.ReceiptNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeePaymentService {

    private final FeePaymentRepository       paymentRepository;
    private final StudentFeeLedgerRepository ledgerRepository;
    private final ReceiptNumberGenerator     receiptGenerator;
    private final FeeLedgerService           ledgerService;
    private final FeeMapper                  mapper;

    // ── Record payment ─────────────────────────────────────────
    @Transactional
    public FeeDto.ReceiptResponse recordPayment(
            FeeDto.RecordPaymentRequest request) {

        UUID schoolId   = TenantContext.getTenantId();
        UUID collectedBy = getCurrentUserId();

        StudentFeeLedger ledger = ledgerRepository
                .findByIdAndSchoolId(request.getLedgerId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Ledger entry not found"));

        // Validate ledger is not already paid or waived
        if (ledger.getStatus() == LedgerStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "This fee is already fully paid");
        }

        if (ledger.getStatus() == LedgerStatus.WAIVED) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "This fee has been waived");
        }

        // Validate payment amount
        BigDecimal balance = ledger.getBalanceDue();
        if (request.getAmountPaid().compareTo(balance) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Payment amount (" + request.getAmountPaid() +
                    ") exceeds balance due (" + balance + ")");
        }

        // Validate cheque fields
        if (request.getPaymentMode() == FeePayment.PaymentMode.CHEQUE &&
                (request.getChequeNo() == null || request.getChequeNo().isBlank())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Cheque number is required for cheque payments");
        }

        // Generate receipt number
        String receiptNo = receiptGenerator.generate(schoolId);

        // Create payment record
        FeePayment payment = FeePayment.builder()
                .schoolId(schoolId)
                .student(ledger.getStudent())
                .ledger(ledger)
                .receiptNo(receiptNo)
                .amountPaid(request.getAmountPaid())
                .paymentMode(request.getPaymentMode())
                .paymentDate(request.getPaymentDate())
                .chequeNo(request.getChequeNo())
                .bankName(request.getBankName())
                .collectedBy(collectedBy)
                .remarks(request.getRemarks())
                .build();

        paymentRepository.save(payment);

        // Update ledger — add paid amount and recalculate status
        ledger.setAmountPaid(ledger.getAmountPaid().add(request.getAmountPaid()));
        ledgerService.updateLedgerStatus(ledger);
        ledgerRepository.save(ledger);

        log.info("Payment recorded: receipt [{}] student [{}] amount [{}]",
                receiptNo, ledger.getStudent().getFullName(),
                request.getAmountPaid());

        return mapper.toReceiptResponse(payment);
    }

    // ── Get receipt by number ──────────────────────────────────
    public FeeDto.ReceiptResponse getByReceiptNo(String receiptNo) {
        UUID schoolId = TenantContext.getTenantId();

        FeePayment payment = paymentRepository
                .findBySchoolIdAndReceiptNo(schoolId, receiptNo)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Receipt not found: " + receiptNo));

        return mapper.toReceiptResponse(payment);
    }

    // ── Get payment history for a ledger entry ─────────────────
    public List<FeeDto.ReceiptResponse> getLedgerPayments(UUID ledgerId) {
        return paymentRepository
                .findByLedger_IdOrderByCreatedAtDesc(ledgerId)
                .stream()
                .map(mapper::toReceiptResponse)
                .collect(Collectors.toList());
    }

    // ── Get all payments for a student ─────────────────────────
    public List<FeeDto.ReceiptResponse> getStudentPayments(UUID studentId) {
        UUID schoolId = TenantContext.getTenantId();
        return paymentRepository
                .findByStudent_IdOrderByPaymentDateDesc(studentId)
                .stream()
                .map(mapper::toReceiptResponse)
                .collect(Collectors.toList());
    }

    // ── Daily collection report ────────────────────────────────
    public FeeDto.DailyCollectionReport getDailyReport(LocalDate date) {
        UUID schoolId = TenantContext.getTenantId();
        LocalDate reportDate = date != null ? date : LocalDate.now();

        List<FeePayment> payments = paymentRepository
                .findBySchoolAndDate(schoolId, reportDate);

        BigDecimal total = payments.stream()
                .map(FeePayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FeeDto.DailyCollectionReport.builder()
                .date(reportDate)
                .totalCollected(total)
                .totalTransactions(payments.size())
                .payments(payments.stream()
                        .map(mapper::toReceiptResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    // ── Private ───────────────────────────────────────────────
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID) {
            return (UUID) auth.getPrincipal();
        }
        return null;
    }
}