package com.yanxitong.payment;

import java.math.BigDecimal;

public record PaymentCallbackResult(
        String orderNo,
        String providerTradeNo,
        BigDecimal paidAmount,
        boolean success,
        String providerEventId,
        String providerSerialNo,
        String eventType,
        String resourceType,
        String decryptedBody
) {
    public PaymentCallbackResult(String orderNo, String providerTradeNo, BigDecimal paidAmount, boolean success) {
        this(orderNo, providerTradeNo, paidAmount, success, null, null, null, null, null);
    }
}
