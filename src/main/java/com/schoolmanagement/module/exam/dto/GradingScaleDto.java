package com.schoolmanagement.module.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class GradingScaleDto {

    @Data
    public static class CreateRequest {

        @NotBlank(message = "Grading scale name is required")
        private String name;

        @NotEmpty(message = "At least one grade level is required")
        @Valid
        private List<LevelRequest> levels;
    }

    @Data
    public static class LevelRequest {

        @NotBlank(message = "Grade label is required")
        private String gradeLabel;

        @NotNull
        private BigDecimal minPercentage;

        @NotNull
        private BigDecimal maxPercentage;

        private BigDecimal gradePoint;
        private String     description;

        @NotNull
        private Integer displayOrder;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID                 id;
        private String               name;
        private Boolean              isActive;
        private List<LevelResponse>  levels;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LevelResponse {
        private UUID       id;
        private String     gradeLabel;
        private BigDecimal minPercentage;
        private BigDecimal maxPercentage;
        private BigDecimal gradePoint;
        private String     description;
        private Integer    displayOrder;
    }
}