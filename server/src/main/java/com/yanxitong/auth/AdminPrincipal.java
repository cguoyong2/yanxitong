package com.yanxitong.auth;

public record AdminPrincipal(Long adminUserId, Long tenantId, String username, String displayName) {
}
