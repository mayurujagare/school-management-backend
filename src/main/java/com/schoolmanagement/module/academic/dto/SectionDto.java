package com.schoolmanagement.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

public class SectionDto {

    @Data
    public static class CreateRequest {

        @NotNull(message = "Grade ID is required")
        private UUID gradeId;

        @NotBlank(message = "Section name is required")
        private String name;

        private Integer capacity;
    }

    @Data
    public static class UpdateRequest {
        private String  name;
        private Integer capacity;
        private Boolean isActive;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID    id;
        private UUID    gradeId;
        private String  gradeName;
        private String  name;
        private Integer capacity;
        private Boolean isActive;
    }
}