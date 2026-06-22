package com.yanxitong.payment;

import java.math.BigDecimal;

public record PaymentCreateCommand(
        String orderNo,
        PaymentScene scene,
        BigDecimal amount,
        String subject,
        Long tenantId,
        Long banquetId,
        String payerOpenId
) {
}

