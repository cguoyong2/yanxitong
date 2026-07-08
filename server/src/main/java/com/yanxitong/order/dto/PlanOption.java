package com.yanxitong.order.dto;

import com.yanxitong.config.entity.PlanRight;
import java.math.BigDecimal;
import java.util.List;

public record PlanOption(
        Long id,
        String planCode,
        String name,
        BigDecimal price,
        String priceUnit,
        Integer recommended,
        Integer sortOrder,
        String status,
        List<PlanRight> rights,
        List<String> rightNames
) {
}
