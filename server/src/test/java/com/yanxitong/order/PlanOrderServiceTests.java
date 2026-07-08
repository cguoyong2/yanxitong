package com.yanxitong.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.config.entity.Plan;
import com.yanxitong.config.entity.PlanRight;
import com.yanxitong.config.mapper.PlanMapper;
import com.yanxitong.config.mapper.PlanRightMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.order.dto.CreatePlanOrderRequest;
import com.yanxitong.order.dto.PlanEntitlementResult;
import com.yanxitong.order.dto.PlanOption;
import com.yanxitong.order.entity.PlanOrder;
import com.yanxitong.order.mapper.PlanOrderMapper;
import com.yanxitong.payment.PaymentService;
import com.yanxitong.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlanOrderServiceTests {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createFreePlanOrderMarksPaidImmediately() {
        TenantContext.setTenantId(1L);
        PlanMapper planMapper = mock(PlanMapper.class);
        PlanRightMapper planRightMapper = mock(PlanRightMapper.class);
        PlanOrderMapper planOrderMapper = mock(PlanOrderMapper.class);
        OrderNoGenerator orderNoGenerator = mock(OrderNoGenerator.class);
        when(planMapper.selectById(10L)).thenReturn(plan("FREE", BigDecimal.ZERO));
        when(planOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(orderNoGenerator.next("PO")).thenReturn("PO202607070001");
        PlanOrderService service = service(planMapper, planRightMapper, planOrderMapper, orderNoGenerator);

        PlanOrder result = service.create(request(100L, 10L));

        ArgumentCaptor<PlanOrder> captor = ArgumentCaptor.forClass(PlanOrder.class);
        verify(planOrderMapper).insert(captor.capture());
        PlanOrder inserted = captor.getValue();
        assertEquals(1L, inserted.tenantId);
        assertEquals(100L, inserted.banquetId);
        assertEquals(10L, inserted.planId);
        assertEquals("PO202607070001", inserted.orderNo);
        assertEquals(BigDecimal.ZERO, inserted.amount);
        assertEquals("场", inserted.priceUnit);
        assertEquals("PAID", inserted.payStatus);
        assertSame(inserted, result);
    }

    @Test
    void createPaidPlanOrderStaysUnpaidAndReusesExistingActiveOrder() {
        PlanMapper planMapper = mock(PlanMapper.class);
        PlanRightMapper planRightMapper = mock(PlanRightMapper.class);
        PlanOrderMapper planOrderMapper = mock(PlanOrderMapper.class);
        OrderNoGenerator orderNoGenerator = mock(OrderNoGenerator.class);
        PlanOrder existing = new PlanOrder();
        existing.id = 99L;
        existing.orderNo = "PO-EXISTING";
        existing.payStatus = "UNPAID";
        when(planMapper.selectById(20L)).thenReturn(plan("PRO", new BigDecimal("199.00")));
        when(planOrderMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        PlanOrderService service = service(planMapper, planRightMapper, planOrderMapper, orderNoGenerator);

        PlanOrder result = service.create(request(100L, 20L));

        assertSame(existing, result);
        verify(planOrderMapper, never()).insert(any(PlanOrder.class));
    }

    @Test
    void mockPaymentSuccessActivatesPaidPlanOnce() {
        PlanMapper planMapper = mock(PlanMapper.class);
        PlanRightMapper planRightMapper = mock(PlanRightMapper.class);
        PlanOrderMapper planOrderMapper = mock(PlanOrderMapper.class);
        PlanOrder order = new PlanOrder();
        order.id = 8L;
        order.orderNo = "PO-1";
        order.payStatus = "UNPAID";
        when(planOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        PlanOrderService service = service(planMapper, planRightMapper, planOrderMapper, mock(OrderNoGenerator.class));

        PlanOrder result = service.mockPaymentSuccess("PO-1");

        assertSame(order, result);
        assertEquals("PAID", order.payStatus);
        verify(planOrderMapper).updateById(order);
    }

    @Test
    void listActivePlanOptionsIncludesConfiguredRights() {
        PlanMapper planMapper = mock(PlanMapper.class);
        PlanRightMapper planRightMapper = mock(PlanRightMapper.class);
        PlanOrderMapper planOrderMapper = mock(PlanOrderMapper.class);
        Plan pro = plan("PRO", new BigDecimal("299.00"));
        pro.id = 20L;
        when(planMapper.selectList(any(Wrapper.class))).thenReturn(List.of(pro));
        PlanRight device = right("DEVICE_RENTAL", "INCLUDED");
        device.planId = 20L;
        device.rightName = "设备租赁";
        PlanRight export = right("EXCEL_EXPORT", "P1_RESERVED");
        export.planId = 20L;
        export.rightName = "Excel 导出";
        when(planRightMapper.selectList(any(Wrapper.class))).thenReturn(List.of(device, export));
        PlanOrderService service = service(planMapper, planRightMapper, planOrderMapper, mock(OrderNoGenerator.class));

        List<PlanOption> result = service.listActivePlanOptions();

        assertEquals(1, result.size());
        assertEquals("PRO", result.get(0).planCode());
        assertEquals(new BigDecimal("299.00"), result.get(0).price());
        assertEquals(List.of("设备租赁", "Excel 导出"), result.get(0).rightNames());
    }

    @Test
    void listActivePlanOptionsHandlesPlansWithoutRights() {
        PlanMapper planMapper = mock(PlanMapper.class);
        PlanRightMapper planRightMapper = mock(PlanRightMapper.class);
        PlanOrderMapper planOrderMapper = mock(PlanOrderMapper.class);
        Plan basic = plan("BASIC", BigDecimal.ZERO);
        basic.id = 10L;
        when(planMapper.selectList(any(Wrapper.class))).thenReturn(List.of(basic));
        when(planRightMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        PlanOrderService service = service(planMapper, planRightMapper, planOrderMapper, mock(OrderNoGenerator.class));

        List<PlanOption> result = service.listActivePlanOptions();

        assertEquals(1, result.size());
        assertTrue(result.get(0).rights().isEmpty());
        assertTrue(result.get(0).rightNames().isEmpty());
    }

    @Test
    void banquetEntitlementsUseLatestPaidOrderRights() {
        PlanMapper planMapper = mock(PlanMapper.class);
        PlanRightMapper planRightMapper = mock(PlanRightMapper.class);
        PlanOrderMapper planOrderMapper = mock(PlanOrderMapper.class);
        PlanOrder paid = new PlanOrder();
        paid.banquetId = 100L;
        paid.planId = 20L;
        paid.payStatus = "PAID";
        when(planOrderMapper.selectOne(any(Wrapper.class))).thenReturn(paid);
        when(planMapper.selectById(20L)).thenReturn(plan("PRO", new BigDecimal("199.00")));
        when(planRightMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                right("DEVICE_RENTAL", "1"),
                right("EXCEL_EXPORT", "1")
        ));
        PlanOrderService service = service(planMapper, planRightMapper, planOrderMapper, mock(OrderNoGenerator.class));

        PlanEntitlementResult result = service.getBanquetEntitlements(100L);

        assertFalse(result.freeDefault());
        assertTrue(result.paidPlanActive());
        assertSame(paid, result.currentOrder());
        assertEquals("1", result.rightValues().get("DEVICE_RENTAL"));
        assertTrue(service.checkBanquetRight(100L, "EXCEL_EXPORT").allowed());
    }

    private PlanOrderService service(
            PlanMapper planMapper,
            PlanRightMapper planRightMapper,
            PlanOrderMapper planOrderMapper,
            OrderNoGenerator orderNoGenerator
    ) {
        return new PlanOrderService(
                planMapper,
                planRightMapper,
                planOrderMapper,
                orderNoGenerator,
                mock(OperationLogService.class),
                mock(PaymentService.class)
        );
    }

    private CreatePlanOrderRequest request(Long banquetId, Long planId) {
        CreatePlanOrderRequest request = new CreatePlanOrderRequest();
        request.banquetId = banquetId;
        request.planId = planId;
        return request;
    }

    private Plan plan(String planCode, BigDecimal price) {
        Plan plan = new Plan();
        plan.id = "FREE".equals(planCode) ? 10L : 20L;
        plan.planCode = planCode;
        plan.name = planCode;
        plan.price = price;
        plan.priceUnit = "场";
        plan.status = "ACTIVE";
        return plan;
    }

    private PlanRight right(String rightCode, String rightValue) {
        PlanRight right = new PlanRight();
        right.rightCode = rightCode;
        right.rightValue = rightValue;
        return right;
    }
}
