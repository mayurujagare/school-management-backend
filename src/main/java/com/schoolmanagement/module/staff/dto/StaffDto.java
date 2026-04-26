package com.schoolmanagement.module.staff.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class StaffDto {

    // ── Create staff member ────────────────────────────────────
    @Data
    public static class CreateRequest {

        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        // At least one required — enforced in service
        private String email;
        private String mobile;

        @NotBlank(message = "Role is required")
        private String role;             // 'TEACHER','CLERK','PRINCIPAL','SCHOOL_ADMIN'

        private String    employeeCode;  // auto-generated if null
        private String    designation;
        private String    department;
        private LocalDate dateOfJoining;
    }

    // ── Update staff ───────────────────────────────────────────
    @Data
    public static class UpdateRequest {
        private String    firstName;
        private String    lastName;
        private String    designation;
        private String    department;
        private LocalDate dateOfJoining;
        private Boolean   isActive;
    }

    // ── Staff response ─────────────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID         id;
        private UUID         userId;
        private String       firstName;
        private String       lastName;
        private String       fullName;
        private String       email;
        private String       mobile;
        private List<String> roles;
        private String       employeeCode;
        private String       designation;
        private String       department;
        private LocalDate    dateOfJoining;
        private Boolean      isActive;
        private LocalDateTime createdAt;
    }

    // ── Assign teacher to section ──────────────────────────────
    @Data
    public static class AssignRequest {
        private UUID    staffId;
        private UUID    sectionId;
        private UUID    subjectId;         // null = class teacher
        private Boolean isClassTeacher;
        private UUID    academicYearId;    // null = current year
    }

    // ── Section teacher response ───────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SectionTeacherResponse {
        private UUID    id;
        private UUID    staffId;
        private String  staffName;
        private UUID    subjectId;
        private String  subjectName;
        private UUID    sectionId;
        private String  sectionName;
        private String  gradeName;
        private Boolean isClassTeacher;
    }
}