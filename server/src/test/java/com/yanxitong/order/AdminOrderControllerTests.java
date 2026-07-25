package com.yanxitong.order;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanxitong.device.DeviceOrderService;
import com.yanxitong.device.entity.DeviceOrder;
import com.yanxitong.order.controller.AdminOrderController;
import com.yanxitong.order.dto.PlanEntitlementResult;
import com.yanxitong.order.entity.PlanOrder;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminOrderControllerTests {
    @Test
    void banquetEntitlementsUseAdminServicePath() {
        PlanOrderService planOrderService = mock(PlanOrderService.class);
        DeviceOrderService deviceOrderService = mock(DeviceOrderService.class);
        PlanEntitlementResult expected = mock(PlanEntitlementResult.class);
        when(planOrderService.getBanquetEntitlements(9L)).thenReturn(expected);
        AdminOrderController controller = new AdminOrderController(planOrderService, deviceOrderService);

        assertSame(expected, controller.banquetEntitlements(9L).data());
        verify(planOrderService).getBanquetEntitlements(9L);
    }

    @Test
    void banquetOrdersAreFilteredByBanquetBeforeReturningToAdmin() {
        PlanOrderService planOrderService = mock(PlanOrderService.class);
        DeviceOrderService deviceOrderService = mock(DeviceOrderService.class);
        List<PlanOrder> planOrders = List.of(mock(PlanOrder.class));
        List<DeviceOrder> deviceOrders = List.of(mock(DeviceOrder.class));
        when(planOrderService.listOrdersByBanquet(7L)).thenReturn(planOrders);
        when(deviceOrderService.listOrdersByBanquet(7L)).thenReturn(deviceOrders);
        AdminOrderController controller = new AdminOrderController(planOrderService, deviceOrderService);

        assertSame(planOrders, controller.banquetPlanOrders(7L).data());
        assertSame(deviceOrders, controller.banquetDeviceOrders(7L).data());
        verify(planOrderService).listOrdersByBanquet(7L);
        verify(deviceOrderService).listOrdersByBanquet(7L);
    }
}
