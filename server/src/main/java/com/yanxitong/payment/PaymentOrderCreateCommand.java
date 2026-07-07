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
        String clientRequestId,
        String bizOrderType,
        String bizOrderNo
) {
    public PaymentOrderCreateCommand(
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
        this(banquetId, scene, entrySource, amount, subject, payerName, payerOpenId, blessing, clientRequestId, null, null);
    }
}
