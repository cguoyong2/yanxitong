package com.yanxitong.device;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.common.PageResult;
import com.yanxitong.device.dto.CreateDeviceOrderRequest;
import com.yanxitong.device.entity.DeviceConfig;
import com.yanxitong.device.entity.DeviceOrder;
import com.yanxitong.device.mapper.DeviceConfigMapper;
import com.yanxitong.device.mapper.DeviceOrderMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.order.OrderNoGenerator;
import com.yanxitong.order.PlanOrderService;
import com.yanxitong.order.dto.RightsCheckResult;
import com.yanxitong.payment.PaymentOrderCreateCommand;
import com.yanxitong.payment.PaymentOrderCreateResult;
import com.yanxitong.payment.PaymentScene;
import com.yanxitong.payment.PaymentService;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DeviceOrderService {
    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.of("CREATED", "CONFIRMED", "DELIVERING", "DELIVERED", "CANCELLED");

    private final DeviceConfigMapper deviceConfigMapper;
    private final DeviceOrderMapper deviceOrderMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final OperationLogService operationLogService;
    private final PlanOrderService planOrderService;
    private final PaymentService paymentService;

    public DeviceOrderService(
            DeviceConfigMapper deviceConfigMapper,
            DeviceOrderMapper deviceOrderMapper,
            OrderNoGenerator orderNoGenerator,
            OperationLogService operationLogService,
            PlanOrderService planOrderService,
            PaymentService paymentService
    ) {
        this.deviceConfigMapper = deviceConfigMapper;
        this.deviceOrderMapper = deviceOrderMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.operationLogService = operationLogService;
        this.planOrderService = planOrderService;
        this.paymentService = paymentService;
    }

    public List<DeviceConfig> listEnabledConfigs() {
        return deviceConfigMapper.selectList(new QueryWrapper<DeviceConfig>()
                .eq("enabled", 1)
                .orderByAsc("device_type"));
    }

    public PageResult<DeviceOrder> listOrders(Integer page, Integer pageSize) {
        QueryWrapper<DeviceOrder> countQuery = tenantScopedDeviceOrderQuery();
        long total = deviceOrderMapper.selectCount(countQuery);
        QueryWrapper<DeviceOrder> query = tenantScopedDeviceOrderQuery();
        int normalizedPageSize = PageResult.normalizePageSize(pageSize);
        query.orderByDesc("created_at").last("LIMIT " + PageResult.offset(page, pageSize) + ", " + normalizedPageSize);
        return PageResult.of(deviceOrderMapper.selectList(query), total, page, pageSize);
    }

    private QueryWrapper<DeviceOrder> tenantScopedDeviceOrderQuery() {
        QueryWrapper<DeviceOrder> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        return query;
    }

    public DeviceOrder create(CreateDeviceOrderRequest request) {
        if (!canRentDevice(request.banquetId, request.deviceType)) {
            throw new IllegalArgumentException("Current plan does not include device rental right");
        }

        DeviceConfig config = deviceConfigMapper.selectOne(new QueryWrapper<DeviceConfig>()
                .eq("device_type", request.deviceType)
                .eq("delivery_method", request.deliveryMethod)
                .eq("enabled", 1)
                .last("LIMIT 1"));
        if (config == null) {
            throw new IllegalArgumentException("Device config not available");
        }
        DeviceOrder existing = findExistingOrder(request);
        if (existing != null) {
            return existing;
        }

        DeviceOrder order = new DeviceOrder();
        order.tenantId = TenantContext.getTenantId();
        order.banquetId = request.banquetId;
        order.orderNo = orderNoGenerator.next("DO");
        order.needDevice = 1;
        order.deviceType = request.deviceType;
        order.rentStartAt = request.rentStartAt;
        order.rentEndAt = request.rentEndAt;
        order.price = config.price;
        order.priceUnit = config.priceUnit;
        order.deliveryMethod = config.deliveryMethod;
        order.payStatus = "UNPAID";
        order.orderStatus = "CREATED";
        deviceOrderMapper.insert(order);
        operationLogService.record(OperationModule.DEVICE, "CREATE_ORDER", "device_order", order.id, "create device order");
        return order;
    }

    public PaymentOrderCreateResult createPaymentOrder(String orderNo, String payerOpenId) {
        DeviceOrder order = findByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Device order not found");
        }
        if ("PAID".equals(order.payStatus)) {
            throw new IllegalArgumentException("Device order already paid");
        }
        if (order.price == null || order.price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Free device order does not need payment");
        }
        return paymentService.createOrder(new PaymentOrderCreateCommand(
                order.banquetId,
                PaymentScene.DEVICE_ORDER,
                order.deviceType,
                order.price,
                "情礼记设备租赁 " + deviceTypeLabel(order.deviceType),
                null,
                payerOpenId,
                null,
                "device-order:" + order.orderNo,
                "DEVICE_ORDER",
                order.orderNo
        ));
    }

    public DeviceOrder fulfillPaidPaymentOrder(PaymentOrder paymentOrder) {
        if (!PaymentScene.DEVICE_ORDER.name().equals(paymentOrder.scene)) {
            throw new IllegalArgumentException("Unsupported device payment scene");
        }
        DeviceOrder order = findByOrderNo(paymentOrder.bizOrderNo);
        if (order == null) {
            throw new IllegalArgumentException("Device order not found");
        }
        if (!"PAID".equals(order.payStatus)) {
            order.payStatus = "PAID";
            order.orderStatus = "CONFIRMED";
            order.updatedAt = LocalDateTime.now();
            deviceOrderMapper.updateById(order);
            operationLogService.record(OperationModule.DEVICE, "PAYMENT_SUCCESS", "device_order", order.id, "device payment success");
        }
        return order;
    }

    public DeviceOrder mockPaymentSuccess(String orderNo) {
        DeviceOrder order = findByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Device order not found");
        }
        if (!"PAID".equals(order.payStatus)) {
            order.payStatus = "PAID";
            order.orderStatus = "CONFIRMED";
            order.updatedAt = LocalDateTime.now();
            deviceOrderMapper.updateById(order);
            operationLogService.record(OperationModule.DEVICE, "MOCK_PAYMENT_SUCCESS", "device_order", order.id, "mock device payment success");
        }
        return order;
    }

    private DeviceOrder findByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return null;
        }
        QueryWrapper<DeviceOrder> query = new QueryWrapper<DeviceOrder>()
                .eq("order_no", orderNo)
                .last("LIMIT 1");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.eq("tenant_id", tenantId);
        }
        return deviceOrderMapper.selectOne(query);
    }

    private String deviceTypeLabel(String value) {
        if ("CLOUD_SPEAKER".equals(value)) {
            return "云喇叭";
        }
        if ("CONFIRM_SCREEN".equals(value)) {
            return "确认屏";
        }
        return value == null ? "设备" : value;
    }

    public DeviceOrder updateOrderStatus(String orderNo, String orderStatus) {
        if (!ALLOWED_ORDER_STATUSES.contains(orderStatus)) {
            throw new IllegalArgumentException("Unsupported device order status");
        }
        DeviceOrder order = deviceOrderMapper.selectOne(new QueryWrapper<DeviceOrder>()
                .eq("order_no", orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new IllegalArgumentException("Device order not found");
        }
        if (!"PAID".equals(order.payStatus) && !"CANCELLED".equals(orderStatus)) {
            throw new IllegalArgumentException("Unpaid device order can only be cancelled");
        }
        String previousStatus = order.orderStatus;
        order.orderStatus = orderStatus;
        order.updatedAt = LocalDateTime.now();
        deviceOrderMapper.updateById(order);
        operationLogService.record(OperationModule.DEVICE, "UPDATE_ORDER_STATUS", "device_order", order.id, "update device order status", Map.of(
                "orderNo", order.orderNo,
                "previousStatus", previousStatus == null ? "" : previousStatus,
                "orderStatus", orderStatus,
                "payStatus", order.payStatus
        ));
        return order;
    }

    public List<DeviceOrder> listOrdersByBanquet(Long banquetId) {
        QueryWrapper<DeviceOrder> query = new QueryWrapper<DeviceOrder>()
                .eq("banquet_id", banquetId)
                .orderByDesc("created_at");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.eq("tenant_id", tenantId);
        }
        return deviceOrderMapper.selectList(query);
    }

    private boolean canRentDevice(Long banquetId, String deviceType) {
        RightsCheckResult generic = planOrderService.checkBanquetRight(banquetId, "DEVICE_RENTAL");
        if (generic.allowed()) {
            return true;
        }
        return planOrderService.checkBanquetRight(banquetId, deviceType).allowed();
    }

    private DeviceOrder findExistingOrder(CreateDeviceOrderRequest request) {
        QueryWrapper<DeviceOrder> query = new QueryWrapper<DeviceOrder>()
                .eq("banquet_id", request.banquetId)
                .eq("device_type", request.deviceType)
                .eq("delivery_method", request.deliveryMethod)
                .in("pay_status", List.of("UNPAID", "PAID"))
                .orderByDesc("updated_at")
                .last("LIMIT 1");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.eq("tenant_id", tenantId);
        }
        if (request.rentStartAt == null) {
            query.isNull("rent_start_at");
        } else {
            query.eq("rent_start_at", request.rentStartAt);
        }
        if (request.rentEndAt == null) {
            query.isNull("rent_end_at");
        } else {
            query.eq("rent_end_at", request.rentEndAt);
        }
        return deviceOrderMapper.selectOne(query);
    }
}
