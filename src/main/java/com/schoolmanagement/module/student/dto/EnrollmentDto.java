// src/main/java/com/schoolmanagement/module/student/dto/EnrollmentDto.java
package com.schoolmanagement.module.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

public class EnrollmentDto {

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID   id;
        private UUID   sectionId;
        private String sectionName;
        private UUID   gradeId;
        private String gradeName;
        private UUID   academicYearId;
        private String academicYearLabel;
        private String rollNumber;
        private Boolean isActive;
    }
}