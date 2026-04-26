package com.schoolmanagement.module.fee.mapper;

import com.schoolmanagement.module.fee.dto.FeeCategoryDto;
import com.schoolmanagement.module.fee.dto.FeeDto;
import com.schoolmanagement.module.fee.dto.FeeStructureDto;
import com.schoolmanagement.module.fee.entity.*;
import org.springframework.stereotype.Component;

@Component
public class FeeMapper {

    public FeeCategoryDto.Response toResponse(FeeCategory fc) {
        return FeeCategoryDto.Response.builder()
                .id(fc.getId())
                .name(fc.getName())
                .description(fc.getDescription())
                .isActive(fc.getIsActive())
                .build();
    }

    public FeeStructureDto.Response toResponse(FeeStructure fs) {
        return FeeStructureDto.Response.builder()
                .id(fs.getId())
                .gradeId(fs.getGrade().getId())
                .gradeName(fs.getGrade().getName())
                .feeCategoryId(fs.getFeeCategory().getId())
                .feeCategoryName(fs.getFeeCategory().getName())
                .amount(fs.getAmount())
                .frequency(fs.getFrequency())
                .dueDay(fs.getDueDay())
                .academicYearLabel(fs.getAcademicYear().getLabel())
                .build();
    }

    public FeeDto.LedgerResponse toResponse(StudentFeeLedger l) {
        return FeeDto.LedgerResponse.builder()
                .id(l.getId())
                .feeCategoryName(l.getFeeStructure().getFeeCategory().getName())
                .frequency(l.getFeeStructure().getFrequency().name())
                .dueDate(l.getDueDate())
                .amountDue(l.getAmountDue())
                .discountName(l.getDiscount() != null
                        ? l.getDiscount().getName() : null)
                .discountType(l.getDiscount() != null
                        ? l.getDiscount().getDiscountType() : null)
                .discountAmount(l.getDiscountAmount())
                .netAmount(l.getNetAmount())
                .amountPaid(l.getAmountPaid())
                .balanceDue(l.getBalanceDue())
                .status(l.getStatus())
                .overdue(l.isOverdue())
                .build();
    }

    public FeeDto.ReceiptResponse toReceiptResponse(FeePayment p) {
        return FeeDto.ReceiptResponse.builder()
                .id(p.getId())
                .receiptNo(p.getReceiptNo())
                .studentName(p.getStudent().getFullName())
                .admissionNo(p.getStudent().getAdmissionNo())
                .feeCategoryName(p.getLedger().getFeeStructure()
                        .getFeeCategory().getName())
                .amountPaid(p.getAmountPaid())
                .paymentMode(p.getPaymentMode())
                .paymentDate(p.getPaymentDate())
                .chequeNo(p.getChequeNo())
                .bankName(p.getBankName())
                .remarks(p.getRemarks())
                .createdAt(p.getCreatedAt())
                .build();
    }

    public FeeDto.DiscountResponse toResponse(FeeDiscount d) {
        return FeeDto.DiscountResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .discountType(d.getDiscountType())
                .value(d.getValue())
                .isActive(d.getIsActive())
                .build();
    }
}