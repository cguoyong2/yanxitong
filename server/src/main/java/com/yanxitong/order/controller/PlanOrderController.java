package com.yanxitong.order.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.config.entity.Plan;
import com.yanxitong.order.PlanOrderService;
import com.yanxitong.order.dto.CreatePlanOrderRequest;
import com.yanxitong.order.dto.PlanEntitlementResult;
import com.yanxitong.order.dto.RightsCheckResult;
import com.yanxitong.order.entity.PlanOrder;
import com.yanxitong.payment.MockPaymentGuard;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanOrderController {
    private final PlanOrderService planOrderService;
    private final MockPaymentGuard mockPaymentGuard;

    public PlanOrderController(PlanOrderService planOrderService, MockPaymentGuard mockPaymentGuard) {
        this.planOrderService = planOrderService;
        this.mockPaymentGuard = mockPaymentGuard;
    }

    @GetMapping
    public ApiResponse<List<Plan>> listPlans() {
        return ApiResponse.ok(planOrderService.listActivePlans());
    }

    @PostMapping("/orders")
    public ApiResponse<PlanOrder> createOrder(@Valid @RequestBody CreatePlanOrderRequest request) {
        return ApiResponse.ok(planOrderService.create(request));
    }

    @GetMapping("/orders")
    public ApiResponse<List<PlanOrder>> orders(@RequestParam Long banquetId) {
        return ApiResponse.ok(planOrderService.listOrdersByBanquet(banquetId));
    }

    @PostMapping("/orders/{orderNo}/mock-success")
    public ApiResponse<PlanOrder> mockPaymentSuccess(@PathVariable String orderNo) {
        mockPaymentGuard.requireEnabled();
        return ApiResponse.ok(planOrderService.mockPaymentSuccess(orderNo));
    }

    @GetMapping("/{planId}/rights/check")
    public ApiResponse<RightsCheckResult> checkRight(@PathVariable Long planId, @RequestParam String rightCode) {
        return ApiResponse.ok(planOrderService.checkRight(planId, rightCode));
    }

    @GetMapping("/banquets/{banquetId}/entitlements")
    public ApiResponse<PlanEntitlementResult> banquetEntitlements(@PathVariable Long banquetId) {
        return ApiResponse.ok(planOrderService.getBanquetEntitlements(banquetId));
    }

    @GetMapping("/banquets/{banquetId}/rights/check")
    public ApiResponse<RightsCheckResult> checkBanquetRight(@PathVariable Long banquetId, @RequestParam String rightCode) {
        return ApiResponse.ok(planOrderService.checkBanquetRight(banquetId, rightCode));
    }
}
