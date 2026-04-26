package com.schoolmanagement.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

public class SubjectDto {

    @Data
    public static class CreateRequest {

        @NotBlank(message = "Subject name is required")
        private String name;

        private String code;
    }

    @Data
    public static class UpdateRequest {
        private String  name;
        private String  code;
        private Boolean isActive;
    }

    @Data
    public static class AssignToGradeRequest {
        private Boolean isMandatory = true;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID    id;
        private String  name;
        private String  code;
        private Boolean isActive;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GradeSubjectResponse {
        private UUID    id;
        private String  name;
        private String  code;
        private Boolean isMandatory;
    }
}