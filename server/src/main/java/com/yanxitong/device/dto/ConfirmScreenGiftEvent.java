package com.yanxitong.device.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfirmScreenGiftEvent(
        String type,
        Long banquetId,
        Long giftRecordId,
        String guestName,
        BigDecimal amount,
        String message,
        LocalDateTime paidAt
) {
}

