package com.yanxitong.order.dto;

import com.yanxitong.config.entity.Plan;
import com.yanxitong.config.entity.PlanRight;
import com.yanxitong.order.entity.PlanOrder;
import java.util.List;
import java.util.Map;

public record PlanEntitlementResult(
        Long banquetId,
        Plan currentPlan,
        PlanOrder currentOrder,
        List<PlanRight> rights,
        Map<String, String> rightValues,
        boolean paidPlanActive,
        boolean freeDefault
) {
}
