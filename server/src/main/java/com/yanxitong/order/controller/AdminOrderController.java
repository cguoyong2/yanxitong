package com.yanxitong.order.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.PageResult;
import com.yanxitong.device.DeviceOrderService;
import com.yanxitong.device.dto.UpdateDeviceOrderStatusRequest;
import com.yanxitong.device.entity.DeviceOrder;
import com.yanxitong.order.PlanOrderService;
import com.yanxitong.order.dto.PlanEntitlementResult;
import com.yanxitong.order.entity.PlanOrder;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final PlanOrderService planOrderService;
    private final DeviceOrderService deviceOrderService;

    public AdminOrderController(PlanOrderService planOrderService, DeviceOrderService deviceOrderService) {
        this.planOrderService = planOrderService;
        this.deviceOrderService = deviceOrderService;
    }

    @GetMapping("/plans")
    public ApiResponse<PageResult<PlanOrder>> planOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(planOrderService.listOrders(page, pageSize));
    }

    @GetMapping("/devices")
    public ApiResponse<PageResult<DeviceOrder>> deviceOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(deviceOrderService.listOrders(page, pageSize));
    }

    @GetMapping("/banquets/{banquetId}/entitlements")
    public ApiResponse<PlanEntitlementResult> banquetEntitlements(@PathVariable Long banquetId) {
        return ApiResponse.ok(planOrderService.getBanquetEntitlements(banquetId));
    }

    @GetMapping("/banquets/{banquetId}/plans")
    public ApiResponse<List<PlanOrder>> banquetPlanOrders(@PathVariable Long banquetId) {
        return ApiResponse.ok(planOrderService.listOrdersByBanquet(banquetId));
    }

    @GetMapping("/banquets/{banquetId}/devices")
    public ApiResponse<List<DeviceOrder>> banquetDeviceOrders(@PathVariable Long banquetId) {
        return ApiResponse.ok(deviceOrderService.listOrdersByBanquet(banquetId));
    }

    @PostMapping("/devices/{orderNo}/status")
    public ApiResponse<DeviceOrder> updateDeviceOrderStatus(
            @PathVariable String orderNo,
            @Valid @RequestBody UpdateDeviceOrderStatusRequest request
    ) {
        return ApiResponse.ok(deviceOrderService.updateOrderStatus(orderNo, request.orderStatus()));
    }
}
