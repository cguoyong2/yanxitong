package com.yanxitong.auth;

public final class AdminPrincipalContext {
    private static final ThreadLocal<AdminPrincipal> HOLDER = new ThreadLocal<>();

    private AdminPrincipalContext() {
    }

    public static void set(AdminPrincipal principal) {
        HOLDER.set(principal);
    }

    public static AdminPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
