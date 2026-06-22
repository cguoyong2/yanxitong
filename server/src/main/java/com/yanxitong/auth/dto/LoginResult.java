package com.yanxitong.auth.dto;

public record LoginResult(String token, Long adminUserId, Long tenantId, String username, String displayName) {
}
