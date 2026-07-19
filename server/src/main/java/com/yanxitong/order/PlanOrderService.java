package com.yanxitong.order;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.common.PageResult;
import com.yanxitong.config.entity.Plan;
import com.yanxitong.config.entity.PlanRight;
import com.yanxitong.config.mapper.PlanMapper;
import com.yanxitong.config.mapper.PlanRightMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.order.dto.CreatePlanOrderRequest;
import com.yanxitong.order.dto.PlanEntitlementResult;
import com.yanxitong.order.dto.PlanOption;
import com.yanxitong.order.dto.RightsCheckResult;
import com.yanxitong.order.entity.PlanOrder;
import com.yanxitong.order.mapper.PlanOrderMapper;
import com.yanxitong.payment.PaymentOrderCreateCommand;
import com.yanxitong.payment.PaymentOrderCreateResult;
import com.yanxitong.payment.PaymentScene;
import com.yanxitong.payment.PaymentService;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PlanOrderService {
    private final PlanMapper planMapper;
    private final PlanRightMapper planRightMapper;
    private final PlanOrderMapper planOrderMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final OperationLogService operationLogService;
    private final PaymentService paymentService;

    public PlanOrderService(
            PlanMapper planMapper,
            PlanRightMapper planRightMapper,
            PlanOrderMapper planOrderMapper,
            OrderNoGenerator orderNoGenerator,
            OperationLogService operationLogService,
            PaymentService paymentService
    ) {
        this.planMapper = planMapper;
        this.planRightMapper = planRightMapper;
        this.planOrderMapper = planOrderMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.operationLogService = operationLogService;
        this.paymentService = paymentService;
    }

    public List<Plan> listActivePlans() {
        return planMapper.selectList(new QueryWrapper<Plan>()
                .eq("status", "ACTIVE")
                .orderByAsc("sort_order"));
    }

    public List<PlanOption> listActivePlanOptions() {
        List<Plan> plans = listActivePlans();
        if (plans.isEmpty()) {
            return List.of();
        }
        List<Long> planIds = plans.stream().map(plan -> plan.id).toList();
        Map<Long, List<PlanRight>> rightsByPlan = planRightMapper.selectList(new QueryWrapper<PlanRight>()
                        .in("plan_id", planIds)
                        .orderByAsc("plan_id")
                        .orderByAsc("right_code"))
                .stream()
                .collect(Collectors.groupingBy(right -> right.planId, LinkedHashMap::new, Collectors.toList()));
        return plans.stream()
                .map(plan -> toPlanOption(plan, rightsByPlan.getOrDefault(plan.id, List.of())))
                .toList();
    }

    private PlanOption toPlanOption(Plan plan, List<PlanRight> rights) {
        return new PlanOption(
                plan.id,
                plan.planCode,
                plan.name,
                plan.price,
                plan.priceUnit,
                plan.recommended,
                plan.sortOrder,
                plan.status,
                rights,
                rights.stream().map(right -> right.rightName).filter(name -> name != null && !name.isBlank()).toList()
        );
    }

    public PageResult<PlanOrder> listOrders(Integer page, Integer pageSize) {
        QueryWrapper<PlanOrder> countQuery = tenantScopedPlanOrderQuery();
        long total = planOrderMapper.selectCount(countQuery);
        QueryWrapper<PlanOrder> query = tenantScopedPlanOrderQuery();
        int normalizedPageSize = PageResult.normalizePageSize(pageSize);
        query.orderByDesc("created_at").last("LIMIT " + PageResult.offset(page, pageSize) + ", " + normalizedPageSize);
        return PageResult.of(planOrderMapper.selectList(query), total, page, pageSize);
    }

    public List<PlanOrder> listOrdersByBanquet(Long banquetId) {
        QueryWrapper<PlanOrder> query = new QueryWrapper<PlanOrder>()
                .eq("banquet_id", banquetId)
                .orderByDesc("updated_at");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        return planOrderMapper.selectList(query);
    }

    private QueryWrapper<PlanOrder> tenantScopedPlanOrderQuery() {
        QueryWrapper<PlanOrder> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        return query;
    }

    public PlanOrder create(CreatePlanOrderRequest request) {
        Plan plan = planMapper.selectById(request.planId);
        if (plan == null || !"ACTIVE".equals(plan.status)) {
            throw new IllegalArgumentException("Plan not available");
        }
        PlanOrder existing = findExistingOrder(request.banquetId, request.planId);
        if (existing != null) {
            return existing;
        }
        PlanOrder order = new PlanOrder();
        order.tenantId = TenantContext.getTenantId();
        order.banquetId = request.banquetId;
        order.planId = request.planId;
        order.orderNo = orderNoGenerator.next("PO");
        order.amount = plan.price;
        order.priceUnit = plan.priceUnit;
        order.payStatus = isFree(plan) ? "PAID" : "UNPAID";
        planOrderMapper.insert(order);
        operationLogService.record(OperationModule.PLAN, "CREATE_ORDER", "plan_order", order.id, "create plan order");
        return order;
    }

    public PaymentOrderCreateResult createPaymentOrder(String orderNo, String payerOpenId) {
        PlanOrder order = findByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Plan order not found");
        }
        if ("PAID".equals(order.payStatus)) {
            throw new IllegalArgumentException("Plan order already paid");
        }
        if (order.amount == null || BigDecimal.ZERO.compareTo(order.amount) >= 0) {
            throw new IllegalArgumentException("Free plan order does not need payment");
        }
        Plan plan = planMapper.selectById(order.planId);
        String subject = "情礼记版本订单 " + (plan == null ? order.orderNo : plan.name);
        return paymentService.createOrder(new PaymentOrderCreateCommand(
                order.banquetId,
                PaymentScene.PLAN_ORDER,
                "PLAN_ORDER",
                order.amount,
                subject,
                null,
                payerOpenId,
                null,
                "plan-order:" + order.orderNo,
                "PLAN_ORDER",
                order.orderNo
        ));
    }

    public PlanOrder fulfillPaidPaymentOrder(PaymentOrder paymentOrder) {
        if (!PaymentScene.PLAN_ORDER.name().equals(paymentOrder.scene)) {
            throw new IllegalArgumentException("Unsupported plan payment scene");
        }
        PlanOrder order = findByOrderNo(paymentOrder.bizOrderNo);
        if (order == null) {
            throw new IllegalArgumentException("Plan order not found");
        }
        if (!"PAID".equals(order.payStatus)) {
            order.payStatus = "PAID";
            order.updatedAt = LocalDateTime.now();
            planOrderMapper.updateById(order);
            operationLogService.record(OperationModule.PLAN, "PAYMENT_SUCCESS", "plan_order", order.id, "plan payment success");
        }
        return order;
    }

    public PlanOrder mockPaymentSuccess(String orderNo) {
        PlanOrder order = findByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Plan order not found");
        }
        if (!"PAID".equals(order.payStatus)) {
            order.payStatus = "PAID";
            planOrderMapper.updateById(order);
            operationLogService.record(OperationModule.PLAN, "MOCK_PAYMENT_SUCCESS", "plan_order", order.id, "mock plan payment success");
        }
        return order;
    }

    private PlanOrder findByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return null;
        }
        QueryWrapper<PlanOrder> query = new QueryWrapper<PlanOrder>()
                .eq("order_no", orderNo)
                .last("LIMIT 1");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        return planOrderMapper.selectOne(query);
    }

    public Long requireOrderBanquetId(String orderNo) {
        PlanOrder order = findByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Plan order not found");
        }
        return order.banquetId;
    }

    public RightsCheckResult checkRight(Long planId, String rightCode) {
        PlanRight right = planRightMapper.selectOne(new QueryWrapper<PlanRight>()
                .eq("plan_id", planId)
                .eq("right_code", rightCode)
                .last("LIMIT 1"));
        return new RightsCheckResult(right != null, rightCode, right == null ? null : right.rightValue);
    }

    public RightsCheckResult checkBanquetRight(Long banquetId, String rightCode) {
        PlanEntitlementResult entitlements = getBanquetEntitlements(banquetId);
        String value = entitlements.rightValues().get(rightCode);
        return new RightsCheckResult(value != null, rightCode, value);
    }

    public PlanEntitlementResult getBanquetEntitlements(Long banquetId) {
        PlanOrder paidOrder = currentPaidOrder(banquetId);
        Plan currentPlan = paidOrder == null ? defaultFreePlan() : planMapper.selectById(paidOrder.planId);
        if (currentPlan == null) {
            return new PlanEntitlementResult(banquetId, null, paidOrder, List.of(), Map.of(), paidOrder != null, paidOrder == null);
        }

        List<PlanRight> rights = planRightMapper.selectList(new QueryWrapper<PlanRight>()
                .eq("plan_id", currentPlan.id)
                .orderByAsc("right_code"));
        Map<String, String> rightValues = new LinkedHashMap<>();
        rights.forEach(right -> rightValues.put(right.rightCode, right.rightValue));
        return new PlanEntitlementResult(
                banquetId,
                currentPlan,
                paidOrder,
                rights,
                rightValues,
                paidOrder != null,
                paidOrder == null
        );
    }

    private PlanOrder currentPaidOrder(Long banquetId) {
        QueryWrapper<PlanOrder> query = new QueryWrapper<PlanOrder>()
                .eq("banquet_id", banquetId)
                .eq("pay_status", "PAID")
                .orderByDesc("updated_at")
                .last("LIMIT 1");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        return planOrderMapper.selectOne(query);
    }

    private Plan defaultFreePlan() {
        return planMapper.selectOne(new QueryWrapper<Plan>()
                .eq("status", "ACTIVE")
                .eq("price", BigDecimal.ZERO)
                .orderByAsc("sort_order")
                .last("LIMIT 1"));
    }

    private boolean isFree(Plan plan) {
        return plan.price == null || BigDecimal.ZERO.compareTo(plan.price) == 0;
    }

    private PlanOrder findExistingOrder(Long banquetId, Long planId) {
        QueryWrapper<PlanOrder> query = new QueryWrapper<PlanOrder>()
                .eq("banquet_id", banquetId)
                .eq("plan_id", planId)
                .in("pay_status", List.of("UNPAID", "PAID"))
                .orderByDesc("updated_at")
                .last("LIMIT 1");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        return planOrderMapper.selectOne(query);
    }
}
