package com.yanxitong.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.device.dto.CreateDeviceOrderRequest;
import com.yanxitong.device.entity.DeviceConfig;
import com.yanxitong.device.entity.DeviceOrder;
import com.yanxitong.device.mapper.DeviceConfigMapper;
import com.yanxitong.device.mapper.DeviceOrderMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.order.OrderNoGenerator;
import com.yanxitong.order.PlanOrderService;
import com.yanxitong.order.dto.RightsCheckResult;
import com.yanxitong.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DeviceOrderServiceTests {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createDeviceOrderRequiresPlanRight() {
        DeviceConfigMapper configMapper = mock(DeviceConfigMapper.class);
        DeviceOrderMapper orderMapper = mock(DeviceOrderMapper.class);
        PlanOrderService planOrderService = mock(PlanOrderService.class);
        when(planOrderService.checkBanquetRight(100L, "DEVICE_RENTAL"))
                .thenReturn(new RightsCheckResult(false, "DEVICE_RENTAL", null));
        when(planOrderService.checkBanquetRight(100L, "CONFIRM_SCREEN"))
                .thenReturn(new RightsCheckResult(false, "CONFIRM_SCREEN", null));
        DeviceOrderService service = service(configMapper, orderMapper, mock(OrderNoGenerator.class), planOrderService);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request())
        );

        assertEquals("Current plan does not include device rental right", ex.getMessage());
        verify(orderMapper, never()).insert(any(DeviceOrder.class));
    }

    @Test
    void createDeviceOrderUsesConfigPriceAndDelivery() {
        TenantContext.setTenantId(1L);
        DeviceConfigMapper configMapper = mock(DeviceConfigMapper.class);
        DeviceOrderMapper orderMapper = mock(DeviceOrderMapper.class);
        OrderNoGenerator orderNoGenerator = mock(OrderNoGenerator.class);
        PlanOrderService planOrderService = mock(PlanOrderService.class);
        when(planOrderService.checkBanquetRight(100L, "DEVICE_RENTAL"))
                .thenReturn(new RightsCheckResult(true, "DEVICE_RENTAL", "1"));
        when(configMapper.selectOne(any(Wrapper.class))).thenReturn(config());
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(orderNoGenerator.next("DO")).thenReturn("DO202607070001");
        DeviceOrderService service = service(configMapper, orderMapper, orderNoGenerator, planOrderService);

        DeviceOrder result = service.create(request());

        ArgumentCaptor<DeviceOrder> captor = ArgumentCaptor.forClass(DeviceOrder.class);
        verify(orderMapper).insert(captor.capture());
        DeviceOrder inserted = captor.getValue();
        assertEquals(1L, inserted.tenantId);
        assertEquals(100L, inserted.banquetId);
        assertEquals("DO202607070001", inserted.orderNo);
        assertEquals(1, inserted.needDevice);
        assertEquals("CONFIRM_SCREEN", inserted.deviceType);
        assertEquals(new BigDecimal("99.00"), inserted.price);
        assertEquals("天", inserted.priceUnit);
        assertEquals("SELF_PICKUP", inserted.deliveryMethod);
        assertEquals("UNPAID", inserted.payStatus);
        assertEquals("CREATED", inserted.orderStatus);
        assertSame(inserted, result);
    }

    @Test
    void createDeviceOrderReusesExistingActiveOrder() {
        DeviceConfigMapper configMapper = mock(DeviceConfigMapper.class);
        DeviceOrderMapper orderMapper = mock(DeviceOrderMapper.class);
        PlanOrderService planOrderService = mock(PlanOrderService.class);
        DeviceOrder existing = new DeviceOrder();
        existing.id = 9L;
        existing.orderNo = "DO-EXISTING";
        when(planOrderService.checkBanquetRight(100L, "DEVICE_RENTAL"))
                .thenReturn(new RightsCheckResult(true, "DEVICE_RENTAL", "1"));
        when(configMapper.selectOne(any(Wrapper.class))).thenReturn(config());
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        DeviceOrderService service = service(configMapper, orderMapper, mock(OrderNoGenerator.class), planOrderService);

        DeviceOrder result = service.create(request());

        assertSame(existing, result);
        verify(orderMapper, never()).insert(any(DeviceOrder.class));
    }

    @Test
    void mockPaymentSuccessConfirmsDeviceOrder() {
        DeviceOrderMapper orderMapper = mock(DeviceOrderMapper.class);
        DeviceOrder order = new DeviceOrder();
        order.id = 9L;
        order.orderNo = "DO-1";
        order.payStatus = "UNPAID";
        order.orderStatus = "CREATED";
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        DeviceOrderService service = service(mock(DeviceConfigMapper.class), orderMapper, mock(OrderNoGenerator.class), mock(PlanOrderService.class));

        DeviceOrder result = service.mockPaymentSuccess("DO-1");

        assertSame(order, result);
        assertEquals("PAID", order.payStatus);
        assertEquals("CONFIRMED", order.orderStatus);
        verify(orderMapper).updateById(order);
    }

    @Test
    void unpaidDeviceOrderCanOnlyBeCancelledByAdmin() {
        DeviceOrderMapper orderMapper = mock(DeviceOrderMapper.class);
        DeviceOrder order = new DeviceOrder();
        order.id = 9L;
        order.orderNo = "DO-1";
        order.payStatus = "UNPAID";
        order.orderStatus = "CREATED";
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        DeviceOrderService service = service(mock(DeviceConfigMapper.class), orderMapper, mock(OrderNoGenerator.class), mock(PlanOrderService.class));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateOrderStatus("DO-1", "DELIVERED")
        );
        assertEquals("Unpaid device order can only be cancelled", ex.getMessage());

        DeviceOrder cancelled = service.updateOrderStatus("DO-1", "CANCELLED");
        assertSame(order, cancelled);
        assertEquals("CANCELLED", order.orderStatus);
        verify(orderMapper).updateById(order);
    }

    private DeviceOrderService service(
            DeviceConfigMapper configMapper,
            DeviceOrderMapper orderMapper,
            OrderNoGenerator orderNoGenerator,
            PlanOrderService planOrderService
    ) {
        return new DeviceOrderService(
                configMapper,
                orderMapper,
                orderNoGenerator,
                mock(OperationLogService.class),
                planOrderService
        );
    }

    private CreateDeviceOrderRequest request() {
        CreateDeviceOrderRequest request = new CreateDeviceOrderRequest();
        request.banquetId = 100L;
        request.deviceType = "CONFIRM_SCREEN";
        request.rentStartAt = LocalDateTime.of(2026, 7, 10, 12, 0);
        request.rentEndAt = LocalDateTime.of(2026, 7, 10, 22, 0);
        request.deliveryMethod = "SELF_PICKUP";
        return request;
    }

    private DeviceConfig config() {
        DeviceConfig config = new DeviceConfig();
        config.deviceType = "CONFIRM_SCREEN";
        config.name = "确认屏";
        config.price = new BigDecimal("99.00");
        config.priceUnit = "天";
        config.deliveryMethod = "SELF_PICKUP";
        config.enabled = 1;
        return config;
    }
}
