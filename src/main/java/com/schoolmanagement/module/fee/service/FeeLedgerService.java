package com.schoolmanagement.module.fee.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.fee.dto.FeeDto;
import com.schoolmanagement.module.fee.entity.*;
import com.schoolmanagement.module.fee.entity.FeeStructure.FeeFrequency;
import com.schoolmanagement.module.fee.entity.StudentFeeLedger.LedgerStatus;
import com.schoolmanagement.module.fee.mapper.FeeMapper;
import com.schoolmanagement.module.fee.repository.*;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.service.AcademicYearService;
import com.schoolmanagement.module.student.entity.Student;
import com.schoolmanagement.module.student.entity.StudentEnrollment;
import com.schoolmanagement.module.student.repository.StudentEnrollmentRepository;
import com.schoolmanagement.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeLedgerService {

    private final StudentFeeLedgerRepository ledgerRepository;
    private final FeeStructureRepository     structureRepository;
    private final FeeDiscountRepository      discountRepository;
    private final StudentRepository          studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearService        academicYearService;
    private final FeeMapper                  mapper;

    // ── Generate ledger entries for a student ─────────────────
    @Transactional
    public FeeDto.StudentFeeSummary generateLedger(
            FeeDto.GenerateLedgerRequest request) {

        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(request.getStudentId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        AcademicYear year = request.getAcademicYearId() != null
                ? academicYearService.getCurrentYearEntity(schoolId)
                : academicYearService.getCurrentYearEntity(schoolId);

        // Get student's grade from current enrollment
        StudentEnrollment enrollment = enrollmentRepository
                .findByStudent_IdAndAcademicYear_Id(student.getId(), year.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Student is not enrolled for this academic year"));

        UUID gradeId = enrollment.getSection().getGrade().getId();

        // Get fee structures for this grade
        List<FeeStructure> structures = structureRepository
                .findByGradeAndYear(schoolId, gradeId, year.getId());

        if (structures.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND,
                    "No fee structure configured for this grade and academic year");
        }

        // Optional discount
        FeeDiscount discount = null;
        if (request.getDiscountId() != null) {
            discount = discountRepository
                    .findByIdAndSchoolId(request.getDiscountId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                            "Discount not found"));
        }

        List<StudentFeeLedger> generatedEntries = new ArrayList<>();

        for (FeeStructure structure : structures) {
            List<LocalDate> dueDates = calculateDueDates(structure, year);

            for (LocalDate dueDate : dueDates) {

                // Skip if already exists
                if (ledgerRepository.existsByStudent_IdAndFeeStructure_IdAndDueDate(
                        student.getId(), structure.getId(), dueDate)) {
                    continue;
                }

                BigDecimal discountAmount = calculateDiscount(
                        structure.getAmount(), discount);
                BigDecimal netAmount = structure.getAmount()
                        .subtract(discountAmount)
                        .max(BigDecimal.ZERO);

                StudentFeeLedger entry = StudentFeeLedger.builder()
                        .schoolId(schoolId)
                        .student(student)
                        .feeStructure(structure)
                        .academicYear(year)
                        .dueDate(dueDate)
                        .amountDue(structure.getAmount())
                        .discount(discount)
                        .discountAmount(discountAmount)
                        .netAmount(netAmount)
                        .build();

                generatedEntries.add(ledgerRepository.save(entry));
            }
        }

        log.info("Generated {} ledger entries for student [{}] year [{}]",
                generatedEntries.size(), student.getFullName(), year.getLabel());

        return buildStudentFeeSummary(student, year, schoolId);
    }

    // ── Get student fee summary ───────────────────────────────
    public FeeDto.StudentFeeSummary getStudentFeeSummary(UUID studentId,
                                                          UUID academicYearId) {
        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        AcademicYear year = academicYearId != null
                ? academicYearService.getCurrentYearEntity(schoolId)
                : academicYearService.getCurrentYearEntity(schoolId);

        return buildStudentFeeSummary(student, year, schoolId);
    }

    // ── Apply discount to ledger entry ────────────────────────
    @Transactional
    public FeeDto.LedgerResponse applyDiscount(
            FeeDto.ApplyDiscountRequest request) {

        UUID schoolId = TenantContext.getTenantId();

        StudentFeeLedger ledger = ledgerRepository
                .findByIdAndSchoolId(request.getLedgerId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Ledger entry not found"));

        if (ledger.getStatus() == LedgerStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot apply discount to a paid ledger entry");
        }

        FeeDiscount discount = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.getDiscountId() != null) {
            discount = discountRepository
                    .findByIdAndSchoolId(request.getDiscountId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                            "Discount not found"));
            discountAmount = calculateDiscount(ledger.getAmountDue(), discount);
        }

        ledger.setDiscount(discount);
        ledger.setDiscountAmount(discountAmount);
        ledger.setNetAmount(ledger.getAmountDue()
                .subtract(discountAmount)
                .max(BigDecimal.ZERO));

        // Recalculate status
        updateLedgerStatus(ledger);

        return mapper.toResponse(ledgerRepository.save(ledger));
    }

    // ── Waive fee ─────────────────────────────────────────────
    @Transactional
    public FeeDto.LedgerResponse waiveFee(FeeDto.WaiveRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        StudentFeeLedger ledger = ledgerRepository
                .findByIdAndSchoolId(request.getLedgerId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Ledger entry not found"));

        if (ledger.getStatus() == LedgerStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot waive a paid ledger entry");
        }

        ledger.setStatus(LedgerStatus.WAIVED);
        ledger.setNetAmount(ledger.getAmountPaid()); // no more balance due

        log.info("Fee waived for student [{}] ledger [{}] reason: {}",
                ledger.getStudent().getFullName(),
                ledger.getId(), request.getReason());

        return mapper.toResponse(ledgerRepository.save(ledger));
    }

    // ── Get overdue fees for school ───────────────────────────
    public List<FeeDto.LedgerResponse> getOverdueFees() {
        UUID schoolId = TenantContext.getTenantId();
        return ledgerRepository
                .findOverdue(schoolId, LocalDate.now())
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Private: calculate due dates ──────────────────────────
    private List<LocalDate> calculateDueDates(FeeStructure structure,
                                               AcademicYear year) {
        List<LocalDate> dates = new ArrayList<>();
        int dueDay = structure.getDueDay() != null
                ? structure.getDueDay() : 10;

        switch (structure.getFrequency()) {
            case ONE_TIME -> dates.add(year.getStartDate().withDayOfMonth(dueDay));

            case ANNUAL   -> dates.add(year.getStartDate().withDayOfMonth(dueDay));

            case MONTHLY  -> {
                LocalDate cursor = year.getStartDate().withDayOfMonth(dueDay);
                while (!cursor.isAfter(year.getEndDate())) {
                    dates.add(cursor);
                    cursor = cursor.plusMonths(1);
                }
            }

            case QUARTERLY -> {
                LocalDate cursor = year.getStartDate().withDayOfMonth(dueDay);
                while (!cursor.isAfter(year.getEndDate())) {
                    dates.add(cursor);
                    cursor = cursor.plusMonths(3);
                }
            }
        }

        return dates;
    }

    // ── Private: calculate discount amount ────────────────────
    private BigDecimal calculateDiscount(BigDecimal amount,
                                          FeeDiscount discount) {
        if (discount == null) return BigDecimal.ZERO;

        return switch (discount.getDiscountType()) {
            case PERCENT -> amount
                    .multiply(discount.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED   -> discount.getValue().min(amount);
        };
    }

    // ── Private: update ledger status after payment ───────────
    public void updateLedgerStatus(StudentFeeLedger ledger) {
        BigDecimal balance = ledger.getNetAmount()
                .subtract(ledger.getAmountPaid());

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            ledger.setStatus(LedgerStatus.PAID);
        } else if (ledger.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            ledger.setStatus(LedgerStatus.PARTIAL);
        } else {
            ledger.setStatus(LedgerStatus.PENDING);
        }
    }

    // ── Private: build student fee summary ────────────────────
    private FeeDto.StudentFeeSummary buildStudentFeeSummary(
            Student student, AcademicYear year, UUID schoolId) {

        List<StudentFeeLedger> entries = ledgerRepository
                .findByStudentAndYear(student.getId(), year.getId());

        BigDecimal totalDue      = BigDecimal.ZERO;
        BigDecimal totalPaid     = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        int pendingCount = 0, paidCount = 0, overdueCount = 0;

        for (StudentFeeLedger e : entries) {
            totalDue      = totalDue.add(e.getNetAmount());
            totalPaid     = totalPaid.add(e.getAmountPaid());
            totalDiscount = totalDiscount.add(e.getDiscountAmount());

            if (e.getStatus() == LedgerStatus.PAID ||
                    e.getStatus() == LedgerStatus.WAIVED) paidCount++;
            else pendingCount++;

            if (e.isOverdue()) overdueCount++;
        }

        return FeeDto.StudentFeeSummary.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .admissionNo(student.getAdmissionNo())
                .academicYearLabel(year.getLabel())
                .totalDue(totalDue)
                .totalPaid(totalPaid)
                .totalBalance(totalDue.subtract(totalPaid))
                .totalDiscount(totalDiscount)
                .pendingCount(pendingCount)
                .paidCount(paidCount)
                .overdueCount(overdueCount)
                .ledger(entries.stream()
                        .map(mapper::toResponse)
                        .collect(Collectors.toList()))
                .build();
    }
}