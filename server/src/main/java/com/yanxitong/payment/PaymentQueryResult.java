package com.yanxitong.payment;

import java.math.BigDecimal;
import java.util.Set;

public record PaymentQueryResult(
        String orderNo,
        String providerTradeNo,
        BigDecimal paidAmount,
        String providerStatus
) {
    private static final Set<String> TERMINAL_UNPAID_STATUSES = Set.of(
            "CLOSED",
            "REVOKED",
            "PAYERROR",
            "REFUND"
    );

    public boolean paid() {
        return "SUCCESS".equals(providerStatus);
    }

    public boolean terminalUnpaid() {
        return TERMINAL_UNPAID_STATUSES.contains(providerStatus);
    }
}
