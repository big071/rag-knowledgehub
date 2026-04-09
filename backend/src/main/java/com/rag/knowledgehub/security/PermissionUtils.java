package com.rag.knowledgehub.security;

public final class PermissionUtils {

    private PermissionUtils() {
    }

    public static boolean isSuperAdmin(String role) {
        return RoleConstants.SUPER_ADMIN.equals(RoleConstants.normalize(role));
    }

    public static boolean isDocAdmin(String role) {
        String normalized = RoleConstants.normalize(role);
        return RoleConstants.DOC_ADMIN.equals(normalized) || RoleConstants.SUPER_ADMIN.equals(normalized);
    }
}
