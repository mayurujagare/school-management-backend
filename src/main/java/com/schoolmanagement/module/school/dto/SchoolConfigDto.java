// src/main/java/com/schoolmanagement/module/school/dto/SchoolConfigDto.java
package com.schoolmanagement.module.school.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

public class SchoolConfigDto {

    // ── Set single config ─────────────────────────────────────
    @Data
    public static class SetRequest {

        @NotBlank(message = "Config key is required")
        private String key;

        @NotBlank(message = "Config value is required")
        private String value;
    }

    // ── Set multiple configs in one call ──────────────────────
    @Data
    public static class BulkSetRequest {
        private Map<String, String> configs;  // key → value
    }

    // ── Config response ───────────────────────────────────────
    @Data
    @Builder
    public static class Response {
        private String key;
        private String value;
    }
}