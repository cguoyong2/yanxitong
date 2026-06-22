package com.yanxitong.payment;

import java.time.LocalDateTime;

public record PaymentCreateResult(
        String orderNo,
        String providerTradeNo,
        String payPayload,
        String prepayId,
        LocalDateTime expiresAt
) {
    public PaymentCreateResult(String orderNo, String providerTradeNo, String payPayload) {
        this(orderNo, providerTradeNo, payPayload, null, null);
    }
}
