package com.schoolmanagement.common.tenant;

import java.util.UUID;

// ThreadLocal store — holds current request's schoolId
// Set by JwtAuthFilter, cleared after request completes
public class TenantContext {

    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) {
        currentTenant.set(tenantId);
    }

    public static UUID getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}