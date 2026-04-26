package com.schoolmanagement.module.fee.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.fee.dto.*;
import com.schoolmanagement.module.fee.entity.FeeDiscount;
import com.schoolmanagement.module.fee.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeConfigService   configService;
    private final FeeLedgerService   ledgerService;
    private final FeePaymentService  paymentService;

    // ─────────────────────────────────────────────────────────
    //  FEE CATEGORIES
    // ─────────────────────────────────────────────────────────

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<FeeCategoryDto.Response>> createCategory(
            @Valid @RequestBody FeeCategoryDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee category created",
                        configService.createCategory(request)));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<List<FeeCategoryDto.Response>>>
            listCategories() {

        return ResponseEntity.ok(
                ApiResponse.success(configService.listCategories()));
    }

    // ─────────────────────────────────────────────────────────
    //  FEE STRUCTURES
    // ─────────────────────────────────────────────────────────

    @PostMapping("/structures")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<FeeStructureDto.Response>> createStructure(
            @Valid @RequestBody FeeStructureDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee structure created",
                        configService.createStructure(request)));
    }

    @GetMapping("/structures")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<List<FeeStructureDto.Response>>>
            listStructures(
                    @RequestParam(required = false) UUID gradeId) {

        return ResponseEntity.ok(
                ApiResponse.success(configService.listStructures(gradeId)));
    }

    // ─────────────────────────────────────────────────────────
    //  FEE DISCOUNTS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/discounts")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<FeeDto.DiscountResponse>> createDiscount(
            @RequestParam String name,
            @RequestParam FeeDiscount.DiscountType type,
            @RequestParam BigDecimal value) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Discount created",
                        configService.createDiscount(name, type, value)));
    }

    @GetMapping("/discounts")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<List<FeeDto.DiscountResponse>>>
            listDiscounts() {

        return ResponseEntity.ok(
                ApiResponse.success(configService.listDiscounts()));
    }

    // ─────────────────────────────────────────────────────────
    //  FEE LEDGER
    // ─────────────────────────────────────────────────────────

    @PostMapping("/ledger/generate")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<FeeDto.StudentFeeSummary>> generateLedger(
            @Valid @RequestBody FeeDto.GenerateLedgerRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee ledger generated",
                        ledgerService.generateLedger(request)));
    }

    @GetMapping("/ledger/student/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<FeeDto.StudentFeeSummary>>
            getStudentFeeSummary(
                    @PathVariable UUID studentId,
                    @RequestParam(required = false) UUID academicYearId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        ledgerService.getStudentFeeSummary(
                                studentId, academicYearId)));
    }

    @PostMapping("/ledger/discount")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<FeeDto.LedgerResponse>> applyDiscount(
            @Valid @RequestBody FeeDto.ApplyDiscountRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Discount applied",
                        ledgerService.applyDiscount(request)));
    }

    @PostMapping("/ledger/waive")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<FeeDto.LedgerResponse>> waiveFee(
            @Valid @RequestBody FeeDto.WaiveRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Fee waived",
                        ledgerService.waiveFee(request)));
    }

    @GetMapping("/ledger/overdue")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<List<FeeDto.LedgerResponse>>>
            getOverdueFees() {

        return ResponseEntity.ok(
                ApiResponse.success(ledgerService.getOverdueFees()));
    }

    // ─────────────────────────────────────────────────────────
    //  PAYMENTS & RECEIPTS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<FeeDto.ReceiptResponse>> recordPayment(
            @Valid @RequestBody FeeDto.RecordPaymentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded",
                        paymentService.recordPayment(request)));
    }

    @GetMapping("/receipts/{receiptNo}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<FeeDto.ReceiptResponse>> getReceipt(
            @PathVariable String receiptNo) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        paymentService.getByReceiptNo(receiptNo)));
    }

    @GetMapping("/payments/student/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<List<FeeDto.ReceiptResponse>>>
            getStudentPayments(@PathVariable UUID studentId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        paymentService.getStudentPayments(studentId)));
    }

    @GetMapping("/payments/ledger/{ledgerId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<List<FeeDto.ReceiptResponse>>>
            getLedgerPayments(@PathVariable UUID ledgerId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        paymentService.getLedgerPayments(ledgerId)));
    }

    @GetMapping("/reports/daily")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<FeeDto.DailyCollectionReport>>
            getDailyReport(
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {

        return ResponseEntity.ok(
                ApiResponse.success(paymentService.getDailyReport(date)));
    }

    // ── Parent: view own child's fee status ────────────────────
    @GetMapping("/my-child/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<FeeDto.StudentFeeSummary>>
            myChildFees(@PathVariable UUID studentId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        ledgerService.getStudentFeeSummary(studentId, null)));
    }
}