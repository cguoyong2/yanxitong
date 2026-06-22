package com.yanxitong.payment;

import java.math.BigDecimal;

public record PaymentOrderCreateCommand(
        Long banquetId,
        PaymentScene scene,
        String entrySource,
        BigDecimal amount,
        String subject,
        String payerName,
        String payerOpenId,
        String blessing,
        String clientRequestId
) {
}
