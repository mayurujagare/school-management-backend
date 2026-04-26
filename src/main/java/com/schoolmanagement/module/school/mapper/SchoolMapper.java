package com.schoolmanagement.module.school.mapper;

import com.schoolmanagement.module.school.dto.AcademicYearDto;
import com.schoolmanagement.module.school.dto.SchoolDto;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.entity.School;
import org.springframework.stereotype.Component;

@Component
public class SchoolMapper {

    public SchoolDto.Response toResponse(School school) {
        return SchoolDto.Response.builder()
                .id(school.getId())
                .name(school.getName())
                .address(school.getAddress())
                .city(school.getCity())
                .state(school.getState())
                .country(school.getCountry())
                .pincode(school.getPincode())
                .phone(school.getPhone())
                .email(school.getEmail())
                .logoUrl(school.getLogoUrl())
                .website(school.getWebsite())
                .isActive(school.getIsActive())
                .subscriptionPlan(school.getSubscriptionPlan())
                .subscriptionStart(school.getSubscriptionStart())
                .subscriptionExpiry(school.getSubscriptionExpiry())
                .subscriptionActive(school.isSubscriptionActive())
                .createdAt(school.getCreatedAt())
                .build();
    }

    public AcademicYearDto.Response toResponse(AcademicYear year) {
        return AcademicYearDto.Response.builder()
                .id(year.getId())
                .label(year.getLabel())
                .startDate(year.getStartDate())
                .endDate(year.getEndDate())
                .isCurrent(year.getIsCurrent())
                .build();
    }
}