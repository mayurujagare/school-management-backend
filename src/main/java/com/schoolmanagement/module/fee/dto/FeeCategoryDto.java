package com.schoolmanagement.module.fee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

public class FeeCategoryDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Category name is required")
        private String name;
        private String description;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID    id;
        private String  name;
        private String  description;
        private Boolean isActive;
    }
}