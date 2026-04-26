// src/main/java/com/schoolmanagement/module/school/dto/AcademicYearDto.java
package com.schoolmanagement.module.school.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

public class AcademicYearDto {

    @Data
    public static class CreateRequest {

        @NotBlank(message = "Label is required")
        private String label;          // '2024-25'

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        private Boolean setAsCurrent = false;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID      id;
        private String    label;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean   isCurrent;
    }
}