package com.yanxitong.device.dto;

public record ConfirmScreenStatusResult(
        Long banquetId,
        String bindCode,
        String bindStatus,
        String deviceType,
        boolean online,
        int onlineSessions
) {
}
