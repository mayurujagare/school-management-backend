package com.schoolmanagement.module.fee.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.entity.Grade;
import com.schoolmanagement.module.academic.repository.GradeRepository;
import com.schoolmanagement.module.fee.dto.FeeCategoryDto;
import com.schoolmanagement.module.fee.dto.FeeDto;
import com.schoolmanagement.module.fee.dto.FeeStructureDto;
import com.schoolmanagement.module.fee.entity.*;
import com.schoolmanagement.module.fee.mapper.FeeMapper;
import com.schoolmanagement.module.fee.repository.*;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.service.AcademicYearService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeConfigService {

    private final FeeCategoryRepository  categoryRepository;
    private final FeeStructureRepository structureRepository;
    private final FeeDiscountRepository  discountRepository;
    private final GradeRepository        gradeRepository;
    private final AcademicYearService    academicYearService;
    private final FeeMapper              mapper;

    // ── Fee Categories ────────────────────────────────────────

    @Transactional
    public FeeCategoryDto.Response createCategory(
            FeeCategoryDto.CreateRequest request) {

        UUID schoolId = TenantContext.getTenantId();

        if (categoryRepository.existsBySchoolIdAndNameIgnoreCase(
                schoolId, request.getName())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Fee category '" + request.getName() + "' already exists");
        }

        FeeCategory category = FeeCategory.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return mapper.toResponse(categoryRepository.save(category));
    }

    public List<FeeCategoryDto.Response> listCategories() {
        UUID schoolId = TenantContext.getTenantId();
        return categoryRepository
                .findBySchoolIdAndIsActiveTrueOrderByNameAsc(schoolId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Fee Structures ────────────────────────────────────────

    @Transactional
    public FeeStructureDto.Response createStructure(
            FeeStructureDto.CreateRequest request) {

        UUID schoolId = TenantContext.getTenantId();

        AcademicYear year = request.getAcademicYearId() != null
                ? academicYearService.getCurrentYearEntity(schoolId)
                : academicYearService.getCurrentYearEntity(schoolId);

        Grade grade = gradeRepository
                .findByIdAndSchoolId(request.getGradeId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grade not found"));

        FeeCategory category = categoryRepository
                .findByIdAndSchoolId(request.getFeeCategoryId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Fee category not found"));

        if (structureRepository
                .existsByAcademicYear_IdAndGrade_IdAndFeeCategory_Id(
                        year.getId(), grade.getId(), category.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Fee structure already exists for this grade and category");
        }

        FeeStructure structure = FeeStructure.builder()
                .schoolId(schoolId)
                .academicYear(year)
                .grade(grade)
                .feeCategory(category)
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .dueDay(request.getDueDay())
                .build();

        return mapper.toResponse(structureRepository.save(structure));
    }

    public List<FeeStructureDto.Response> listStructures(UUID gradeId) {
        UUID schoolId = TenantContext.getTenantId();
        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        List<FeeStructure> structures = gradeId != null
                ? structureRepository.findByGradeAndYear(
                        schoolId, gradeId, year.getId())
                : structureRepository.findBySchoolAndYear(
                        schoolId, year.getId());

        return structures.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Fee Discounts ─────────────────────────────────────────

    @Transactional
    public FeeDto.DiscountResponse createDiscount(String name,
                                                   FeeDiscount.DiscountType type,
                                                   BigDecimal value) {
        UUID schoolId = TenantContext.getTenantId();

        FeeDiscount discount = FeeDiscount.builder()
                .schoolId(schoolId)
                .name(name)
                .discountType(type)
                .value(value)
                .build();

        return mapper.toResponse(discountRepository.save(discount));
    }

    public List<FeeDto.DiscountResponse> listDiscounts() {
        UUID schoolId = TenantContext.getTenantId();
        return discountRepository
                .findBySchoolIdAndIsActiveTrueOrderByNameAsc(schoolId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}