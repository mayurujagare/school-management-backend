package com.schoolmanagement.module.timetable.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.timetable.dto.TimetableDto;
import com.schoolmanagement.module.timetable.entity.TimetablePeriod;
import com.schoolmanagement.module.timetable.mapper.TimetableMapper;
import com.schoolmanagement.module.timetable.repository.TimetablePeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetablePeriodService {

    private final TimetablePeriodRepository periodRepository;
    private final TimetableMapper           mapper;

    @Transactional
    public TimetableDto.PeriodResponse create(TimetableDto.CreatePeriodRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        if (request.getEndTime().isBefore(request.getStartTime()) ||
                request.getEndTime().equals(request.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "End time must be after start time");
        }

        if (periodRepository.existsBySchoolIdAndNameIgnoreCase(
                schoolId, request.getName())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Period name '" + request.getName() + "' already exists");
        }

        if (periodRepository.existsBySchoolIdAndDisplayOrder(
                schoolId, request.getDisplayOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Display order " + request.getDisplayOrder() + " already in use");
        }

        TimetablePeriod period = TimetablePeriod.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isBreak(Boolean.TRUE.equals(request.getIsBreak()))
                .displayOrder(request.getDisplayOrder())
                .build();

        TimetablePeriod saved = periodRepository.save(period);
        log.info("Period created: {} [{} - {}]",
                saved.getName(), saved.getStartTime(), saved.getEndTime());

        return mapper.toResponse(saved);
    }

    public List<TimetableDto.PeriodResponse> listAll() {
        UUID schoolId = TenantContext.getTenantId();
        return periodRepository
                .findBySchoolIdOrderByDisplayOrderAsc(schoolId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TimetableDto.PeriodResponse update(UUID periodId,
                                              TimetableDto.UpdatePeriodRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        TimetablePeriod period = periodRepository
                .findByIdAndSchoolId(periodId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Period not found"));

        if (request.getName()         != null) period.setName(request.getName());
        if (request.getStartTime()    != null) period.setStartTime(request.getStartTime());
        if (request.getEndTime()      != null) period.setEndTime(request.getEndTime());
        if (request.getIsBreak()      != null) period.setIsBreak(request.getIsBreak());
        if (request.getDisplayOrder() != null) period.setDisplayOrder(request.getDisplayOrder());

        if (period.getEndTime().isBefore(period.getStartTime()) ||
                period.getEndTime().equals(period.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "End time must be after start time");
        }

        return mapper.toResponse(periodRepository.save(period));
    }

    public TimetablePeriod validateAndGet(UUID periodId, UUID schoolId) {
        return periodRepository.findByIdAndSchoolId(periodId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Period not found"));
    }
}