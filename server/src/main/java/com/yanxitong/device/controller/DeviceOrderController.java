package com.yanxitong.device.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.banquet.BanquetAccessService;
import com.yanxitong.miniapp.MiniappAuthenticated;
import com.yanxitong.device.DeviceOrderService;
import com.yanxitong.device.dto.CreateDeviceOrderRequest;
import com.yanxitong.device.entity.DeviceConfig;
import com.yanxitong.device.entity.DeviceOrder;
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
@RequestMapping("/api/devices")
public class DeviceOrderController {
    private final DeviceOrderService deviceOrderService;
    private final MockPaymentGuard mockPaymentGuard;
    private final BanquetAccessService banquetAccessService;

    public DeviceOrderController(
            DeviceOrderService deviceOrderService,
            MockPaymentGuard mockPaymentGuard,
            BanquetAccessService banquetAccessService
    ) {
        this.deviceOrderService = deviceOrderService;
        this.mockPaymentGuard = mockPaymentGuard;
        this.banquetAccessService = banquetAccessService;
    }

    @GetMapping("/configs")
    public ApiResponse<List<DeviceConfig>> configs() {
        return ApiResponse.ok(deviceOrderService.listEnabledConfigs());
    }

    @PostMapping("/orders")
    @MiniappAuthenticated
    public ApiResponse<DeviceOrder> createOrder(@Valid @RequestBody CreateDeviceOrderRequest request) {
        banquetAccessService.requireAccessible(request.banquetId);
        return ApiResponse.ok(deviceOrderService.create(request));
    }

    @GetMapping("/orders")
    @MiniappAuthenticated
    public ApiResponse<List<DeviceOrder>> orders(@RequestParam Long banquetId) {
        banquetAccessService.requireAccessible(banquetId);
        return ApiResponse.ok(deviceOrderService.listOrdersByBanquet(banquetId));
    }

    @PostMapping("/orders/{orderNo}/mock-success")
    @MiniappAuthenticated
    public ApiResponse<DeviceOrder> mockPaymentSuccess(@PathVariable String orderNo) {
        banquetAccessService.requireAccessible(deviceOrderService.requireOrderBanquetId(orderNo));
        mockPaymentGuard.requireEnabled();
        return ApiResponse.ok(deviceOrderService.mockPaymentSuccess(orderNo));
    }

    @PostMapping("/orders/{orderNo}/payment")
    @MiniappAuthenticated
    public ApiResponse<PaymentOrderCreateResult> createPayment(
            @PathVariable String orderNo,
            @Valid @RequestBody CreateBusinessPaymentRequest request
    ) {
        banquetAccessService.requireAccessible(deviceOrderService.requireOrderBanquetId(orderNo));
        return ApiResponse.ok(deviceOrderService.createPaymentOrder(orderNo, request.payerOpenId));
    }
}
