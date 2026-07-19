package com.yanxitong.order.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.banquet.BanquetAccessService;
import com.yanxitong.miniapp.MiniappAuthenticated;
import com.yanxitong.order.PlanOrderService;
import com.yanxitong.order.dto.CreatePlanOrderRequest;
import com.yanxitong.order.dto.PlanEntitlementResult;
import com.yanxitong.order.dto.PlanOption;
import com.yanxitong.order.dto.RightsCheckResult;
import com.yanxitong.order.entity.PlanOrder;
import com.yanxitong.payment.MockPaymentGuard;
import com.yanxitong.payment.PaymentOrderCreateResult;
import com.yanxitong.payment.dto.CreateBusinessPaymentRequest;
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
    private final BanquetAccessService banquetAccessService;

    public PlanOrderController(
            PlanOrderService planOrderService,
            MockPaymentGuard mockPaymentGuard,
            BanquetAccessService banquetAccessService
    ) {
        this.planOrderService = planOrderService;
        this.mockPaymentGuard = mockPaymentGuard;
        this.banquetAccessService = banquetAccessService;
    }

    @GetMapping
    public ApiResponse<List<PlanOption>> listPlans() {
        return ApiResponse.ok(planOrderService.listActivePlanOptions());
    }

    @PostMapping("/orders")
    @MiniappAuthenticated
    public ApiResponse<PlanOrder> createOrder(@Valid @RequestBody CreatePlanOrderRequest request) {
        banquetAccessService.requireAccessible(request.banquetId);
        return ApiResponse.ok(planOrderService.create(request));
    }

    @GetMapping("/orders")
    @MiniappAuthenticated
    public ApiResponse<List<PlanOrder>> orders(@RequestParam Long banquetId) {
        banquetAccessService.requireAccessible(banquetId);
        return ApiResponse.ok(planOrderService.listOrdersByBanquet(banquetId));
    }

    @PostMapping("/orders/{orderNo}/mock-success")
    @MiniappAuthenticated
    public ApiResponse<PlanOrder> mockPaymentSuccess(@PathVariable String orderNo) {
        banquetAccessService.requireAccessible(planOrderService.requireOrderBanquetId(orderNo));
        mockPaymentGuard.requireEnabled();
        return ApiResponse.ok(planOrderService.mockPaymentSuccess(orderNo));
    }

    @PostMapping("/orders/{orderNo}/payment")
    @MiniappAuthenticated
    public ApiResponse<PaymentOrderCreateResult> createPayment(
            @PathVariable String orderNo,
            @Valid @RequestBody CreateBusinessPaymentRequest request
    ) {
        banquetAccessService.requireAccessible(planOrderService.requireOrderBanquetId(orderNo));
        return ApiResponse.ok(planOrderService.createPaymentOrder(orderNo, request.payerOpenId));
    }

    @GetMapping("/{planId}/rights/check")
    public ApiResponse<RightsCheckResult> checkRight(@PathVariable Long planId, @RequestParam String rightCode) {
        return ApiResponse.ok(planOrderService.checkRight(planId, rightCode));
    }

    @GetMapping("/banquets/{banquetId}/entitlements")
    @MiniappAuthenticated
    public ApiResponse<PlanEntitlementResult> banquetEntitlements(@PathVariable Long banquetId) {
        banquetAccessService.requireAccessible(banquetId);
        return ApiResponse.ok(planOrderService.getBanquetEntitlements(banquetId));
    }

    @GetMapping("/banquets/{banquetId}/rights/check")
    @MiniappAuthenticated
    public ApiResponse<RightsCheckResult> checkBanquetRight(@PathVariable Long banquetId, @RequestParam String rightCode) {
        banquetAccessService.requireAccessible(banquetId);
        return ApiResponse.ok(planOrderService.checkBanquetRight(banquetId, rightCode));
    }
}
