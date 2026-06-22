package com.yanxitong.gift.dto;

import java.math.BigDecimal;
import java.util.Map;

public record GiftSummaryResult(
        Long banquetId,
        long totalRecords,
        BigDecimal totalAmount,
        Map<String, Long> sourceCounts,
        Map<String, BigDecimal> sourceAmounts
) {
}
