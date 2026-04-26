package com.schoolmanagement.module.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

public class ExamTypeDto {

    @Data
    public static class CreateRequest {

        @NotBlank(message = "Exam type name is required")
        private String name;

        @NotNull
        @DecimalMin(value = "1.00", message = "Max marks must be at least 1")
        private BigDecimal maxMarks;

        private BigDecimal weightage;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID       id;
        private String     name;
        private BigDecimal maxMarks;
        private BigDecimal weightage;
        private Boolean    isActive;
    }
}