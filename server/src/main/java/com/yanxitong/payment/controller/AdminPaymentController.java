package com.yanxitong.payment.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.PageResult;
import com.yanxitong.payment.PaymentCallbackService;
import com.yanxitong.payment.PaymentProvider;
import com.yanxitong.payment.PaymentProviderReadinessService;
import com.yanxitong.payment.dto.ManualSettlePaymentOrderRequest;
import com.yanxitong.payment.dto.PaymentLaunchReadiness;
import com.yanxitong.payment.dto.PaymentProviderStatus;
import com.yanxitong.payment.dto.ResolvePaymentCallbackRequest;
import com.yanxitong.payment.entity.PaymentCallbackLog;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.gift.entity.GiftRecord;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {
    private final PaymentCallbackService paymentCallbackService;
    private final PaymentProviderReadinessService readinessService;

    public AdminPaymentController(
            PaymentCallbackService paymentCallbackService,
            PaymentProviderReadinessService readinessService
    ) {
        this.paymentCallbackService = paymentCallbackService;
        this.readinessService = readinessService;
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<PaymentOrder>> orders(
            @RequestParam(required = false) String payStatus,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(paymentCallbackService.listOrders(payStatus, scene, page, pageSize));
    }

    @GetMapping("/providers")
    public ApiResponse<List<PaymentProviderStatus>> providers() {
        return ApiResponse.ok(List.of(PaymentProvider.values()).stream()
                .map(readinessService::status)
                .toList());
    }

    @GetMapping("/launch-readiness")
    public ApiResponse<PaymentLaunchReadiness> launchReadiness(
            @RequestParam(defaultValue = "WECHAT_SERVICE_PROVIDER") PaymentProvider provider
    ) {
        return ApiResponse.ok(readinessService.launchReadiness(provider));
    }

    @GetMapping("/callbacks")
    public ApiResponse<PageResult<PaymentCallbackLog>> callbacks(
            @RequestParam(required = false) String processStatus,
            @RequestParam(required = false) String verifyStatus,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(paymentCallbackService.listCallbacks(processStatus, verifyStatus, page, pageSize));
    }

    @PostMapping("/callbacks/{id}/resolve")
    public ApiResponse<PaymentCallbackLog> resolveCallback(
            @PathVariable Long id,
            @RequestBody ResolvePaymentCallbackRequest request
    ) {
        return ApiResponse.ok(paymentCallbackService.resolveCallback(id, request));
    }

    @PostMapping("/callbacks/{id}/retry")
    public ApiResponse<PaymentCallbackLog> retryCallback(
            @PathVariable Long id,
            @RequestBody(required = false) ResolvePaymentCallbackRequest request
    ) {
        return ApiResponse.ok(paymentCallbackService.retryCallback(id, request));
    }

    @PostMapping("/orders/{id}/compensate-fulfillment")
    public ApiResponse<GiftRecord> compensateFulfillment(
            @PathVariable Long id,
            @RequestBody(required = false) ResolvePaymentCallbackRequest request
    ) {
        return ApiResponse.ok(paymentCallbackService.compensateFulfillment(id, request));
    }

    @PostMapping("/orders/{id}/manual-settle")
    public ApiResponse<GiftRecord> manualSettleOrder(
            @PathVariable Long id,
            @RequestBody ManualSettlePaymentOrderRequest request
    ) {
        return ApiResponse.ok(paymentCallbackService.manualSettleOrder(id, request));
    }

}
