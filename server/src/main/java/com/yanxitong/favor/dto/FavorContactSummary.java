package com.yanxitong.favor.dto;

import java.math.BigDecimal;

public record FavorContactSummary(
        Long contactId,
        String contactName,
        BigDecimal receivedAmount,
        BigDecimal givenAmount,
        BigDecimal balance
) {
}

