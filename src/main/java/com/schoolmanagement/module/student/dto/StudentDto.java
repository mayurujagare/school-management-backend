package com.schoolmanagement.module.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class StudentDto {

    // ── Enroll new student ─────────────────────────────────────
    @Data
    public static class EnrollRequest {

        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        private LocalDate dateOfBirth;
        private String    gender;
        private String    address;
        private String    bloodGroup;
        private String    admissionNo;         // optional — auto-generated if null

        // Section + year to enroll into
        private UUID      sectionId;           // optional at enrollment time
        private String    rollNumber;

        // Parents — min 1, max 2
        @NotEmpty(message = "At least one parent is required")
        @Size(max = 2, message = "Maximum 2 parents allowed")
        @Valid
        private List<ParentDto.CreateRequest> parents;
    }

    // ── Update student details ─────────────────────────────────
    @Data
    public static class UpdateRequest {
        private String    firstName;
        private String    lastName;
        private LocalDate dateOfBirth;
        private String    gender;
        private String    address;
        private String    bloodGroup;
        private String    photoUrl;
        private String    admissionNo;
        private Boolean   isActive;
    }

    // ── Student response ───────────────────────────────────────
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID                      id;
        private String                    admissionNo;
        private String                    firstName;
        private String                    lastName;
        private String                    fullName;
        private LocalDate                 dateOfBirth;
        private String                    gender;
        private String                    address;
        private String                    photoUrl;
        private String                    bloodGroup;
        private LocalDate                 enrolledAt;
        private Boolean                   isActive;
        private List<ParentDto.Response>  parents;
        private EnrollmentDto.Response    currentEnrollment;
        private LocalDateTime             createdAt;
    }

    // ── Enrollment into section ────────────────────────────────
    @Data
    public static class EnrollToSectionRequest {

        private UUID   sectionId;
        private String rollNumber;

        // Optional — if null, current academic year is used
        private UUID   academicYearId;
    }
}