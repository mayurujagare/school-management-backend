package com.schoolmanagement.module.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

public class ParentDto {

    // ── Create parent (used inside EnrollRequest) ──────────────
    @Data
    public static class CreateRequest {

        @NotBlank(message = "Parent first name is required")
        private String firstName;

        @NotBlank(message = "Parent last name is required")
        private String lastName;

        // At least one of email or mobile is required
        // Enforced in service layer
        private String email;
        private String mobile;

        private String  relation;             // 'FATHER','MOTHER','GUARDIAN'
        private String  occupation;
        private Boolean isPrimary = false;
    }

    // ── Add parent to existing student ─────────────────────────
    @Data
    public static class AddRequest {
        private UUID    existingParentId;     // link existing parent, OR
        private CreateRequest newParent;      // create a new one
        private Boolean isPrimary = false;
    }

    // ── Parent response ────────────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID               id;
        private String             firstName;
        private String             lastName;
        private String             fullName;
        private String             email;
        private String             mobile;
        private String             relation;
        private String             occupation;
        private Boolean            isPrimary;
        private List<StudentSummary> linkedStudents;  // shown on parent dashboard
    }

    // ── Lightweight student summary for parent view ────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudentSummary {
        private UUID   id;
        private String admissionNo;
        private String firstName;
        private String lastName;
        private String fullName;
        private String photoUrl;
        private String gradeName;
        private String sectionName;
    }
}