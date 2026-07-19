package com.yanxitong.miniapp;

public final class MiniappPrincipalContext {
    private static final ThreadLocal<MiniappPrincipal> HOLDER = new ThreadLocal<>();

    private MiniappPrincipalContext() {
    }

    public static void set(MiniappPrincipal principal) {
        HOLDER.set(principal);
    }

    public static MiniappPrincipal get() {
        return HOLDER.get();
    }

    public static Long currentUserId() {
        MiniappPrincipal principal = HOLDER.get();
        return principal == null ? null : principal.userId();
    }

    public static Long requireUserId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new IllegalStateException("Miniapp user is not authenticated");
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
