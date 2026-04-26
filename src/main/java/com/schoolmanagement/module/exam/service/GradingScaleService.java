package com.schoolmanagement.module.exam.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.exam.dto.GradingScaleDto;
import com.schoolmanagement.module.exam.entity.GradingScale;
import com.schoolmanagement.module.exam.entity.GradingScaleLevel;
import com.schoolmanagement.module.exam.mapper.ExamMapper;
import com.schoolmanagement.module.exam.repository.GradingScaleLevelRepository;
import com.schoolmanagement.module.exam.repository.GradingScaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingScaleService {

    private final GradingScaleRepository       scaleRepository;
    private final GradingScaleLevelRepository  levelRepository;
    private final ExamMapper                   mapper;

    @Transactional
    public GradingScaleDto.Response create(GradingScaleDto.CreateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        GradingScale scale = GradingScale.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .build();
        scale = scaleRepository.save(scale);

        for (GradingScaleDto.LevelRequest level : request.getLevels()) {
            if (level.getMinPercentage().compareTo(level.getMaxPercentage()) > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Min percentage cannot be greater than max for " +
                        level.getGradeLabel());
            }

            GradingScaleLevel l = GradingScaleLevel.builder()
                    .gradingScale(scale)
                    .gradeLabel(level.getGradeLabel())
                    .minPercentage(level.getMinPercentage())
                    .maxPercentage(level.getMaxPercentage())
                    .gradePoint(level.getGradePoint())
                    .description(level.getDescription())
                    .displayOrder(level.getDisplayOrder())
                    .build();
            levelRepository.save(l);
        }

        return getById(scale.getId());
    }

    public List<GradingScaleDto.Response> listAll() {
        UUID schoolId = TenantContext.getTenantId();
        return scaleRepository
                .findBySchoolIdAndIsActiveTrueOrderByNameAsc(schoolId)
                .stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    public GradingScaleDto.Response getById(UUID scaleId) {
        UUID schoolId = TenantContext.getTenantId();
        GradingScale scale = scaleRepository
                .findByIdAndSchoolId(scaleId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Grading scale not found"));
        return buildResponse(scale);
    }

    private GradingScaleDto.Response buildResponse(GradingScale scale) {
        List<GradingScaleLevel> levels = levelRepository
                .findByGradingScale_IdOrderByDisplayOrderAsc(scale.getId());
        return mapper.toResponse(scale, levels);
    }

    public List<GradingScaleLevel> getLevels(UUID scaleId) {
        return levelRepository
                .findByGradingScale_IdOrderByDisplayOrderAsc(scaleId);
    }
}