package com.schoolmanagement.module.exam.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.exam.dto.ExamTypeDto;
import com.schoolmanagement.module.exam.entity.ExamType;
import com.schoolmanagement.module.exam.mapper.ExamMapper;
import com.schoolmanagement.module.exam.repository.ExamTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamTypeService {

    private final ExamTypeRepository repository;
    private final ExamMapper          mapper;

    @Transactional
    public ExamTypeDto.Response create(ExamTypeDto.CreateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        if (repository.existsBySchoolIdAndNameIgnoreCase(
                schoolId, request.getName())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Exam type already exists: " + request.getName());
        }

        ExamType examType = ExamType.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .maxMarks(request.getMaxMarks())
                .weightage(request.getWeightage())
                .build();

        return mapper.toResponse(repository.save(examType));
    }

    public List<ExamTypeDto.Response> listAll() {
        UUID schoolId = TenantContext.getTenantId();
        return repository
                .findBySchoolIdAndIsActiveTrueOrderByNameAsc(schoolId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}