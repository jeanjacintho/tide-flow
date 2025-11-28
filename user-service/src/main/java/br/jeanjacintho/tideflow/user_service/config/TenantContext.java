package br.jeanjacintho.tideflow.user_service.config;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> COMPANY_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> DEPARTMENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> COMPANY_ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> SYSTEM_ROLE = new ThreadLocal<>();

    public static void setCompanyId(UUID companyId) {
        COMPANY_ID.set(companyId);
    }

    public static UUID getCompanyId() {
        return COMPANY_ID.get();
    }

    public static void setDepartmentId(UUID departmentId) {
        DEPARTMENT_ID.set(departmentId);
    }

    public static UUID getDepartmentId() {
        return DEPARTMENT_ID.get();
    }

    public static void setCompanyRole(String role) {
        COMPANY_ROLE.set(role);
    }

    public static String getCompanyRole() {
        return COMPANY_ROLE.get();
    }

    public static void setSystemRole(String systemRole) {
        SYSTEM_ROLE.set(systemRole);
    }

    public static String getSystemRole() {
        return SYSTEM_ROLE.get();
    }

    public static boolean isSystemAdmin() {
        return "SYSTEM_ADMIN".equals(SYSTEM_ROLE.get());
    }

    public static void clear() {
        COMPANY_ID.remove();
        DEPARTMENT_ID.remove();
        COMPANY_ROLE.remove();
        SYSTEM_ROLE.remove();
    }
}
