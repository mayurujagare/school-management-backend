package com.schoolmanagement.module.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuperAdminDto {

    // ── Platform-wide dashboard stats ─────────────────────────
    @Data
    @Builder
    public static class PlatformStats {
        private long totalSchools;
        private long activeSchools;
        private long inactiveSchools;
        private long demoSchools;
        private long expiringThisMonth;   // subscriptions expiring in 30 days
    }
}