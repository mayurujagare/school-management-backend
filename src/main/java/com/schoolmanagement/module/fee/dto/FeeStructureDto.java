package com.schoolmanagement.module.fee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolmanagement.module.fee.entity.FeeStructure.FeeFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

public class FeeStructureDto {

    @Data
    public static class CreateRequest {

        @NotNull(message = "Grade is required")
        private UUID gradeId;

        @NotNull(message = "Fee category is required")
        private UUID feeCategoryId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;

        @NotNull(message = "Frequency is required")
        private FeeFrequency frequency;

        private Integer dueDay;           // 1-28 for MONTHLY

        private UUID academicYearId;      // null = current year
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID         id;
        private UUID         gradeId;
        private String       gradeName;
        private UUID         feeCategoryId;
        private String       feeCategoryName;
        private BigDecimal   amount;
        private FeeFrequency frequency;
        private Integer      dueDay;
        private String       academicYearLabel;
    }
}