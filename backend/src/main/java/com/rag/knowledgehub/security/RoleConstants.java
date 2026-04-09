package com.rag.knowledgehub.security;

public final class RoleConstants {

    public static final String USER = "USER";
    public static final String DOC_ADMIN = "DOC_ADMIN";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String LEGACY_ADMIN = "ADMIN";

    private RoleConstants() {
    }

    public static String normalize(String role) {
        if (LEGACY_ADMIN.equals(role)) {
            return DOC_ADMIN;
        }
        return role;
    }
}
