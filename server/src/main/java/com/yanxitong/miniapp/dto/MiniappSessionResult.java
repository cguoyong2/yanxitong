package com.yanxitong.miniapp.dto;

public record MiniappSessionResult(
        String token,
        long expiresInSeconds,
        Long userId,
        String openId,
        String roleCode
) {
}
