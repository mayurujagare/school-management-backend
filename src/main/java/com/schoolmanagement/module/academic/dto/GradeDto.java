package com.schoolmanagement.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

public class GradeDto {

    @Data
    public static class CreateRequest {

        @NotBlank(message = "Grade name is required")
        private String name;

        @NotNull(message = "Display order is required")
        @Min(value = 1, message = "Display order must be at least 1")
        private Integer displayOrder;
    }

    @Data
    public static class UpdateRequest {
        private String  name;
        private Integer displayOrder;
        private Boolean isActive;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID              id;
        private String            name;
        private Integer           displayOrder;
        private Boolean           isActive;
        private List<SectionDto.Response> sections;   // optional, populated on demand
    }
}