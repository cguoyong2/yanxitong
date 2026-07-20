package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentMaintenanceServiceTests {
    @Test
    void paidProviderQueryUsesExistingFulfillmentPath() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
        PaymentOrder order = pendingOrder(now.minusMinutes(5), now.plusMinutes(25));
        PaymentOrderMapper mapper = mapperWith(order);
        CapturingAdapter adapter = new CapturingAdapter(new PaymentQueryResult(
                order.orderNo,
                "WX-PAID-1",
                order.amount,
                "SUCCESS"
        ));
        PaymentCallbackService callbackService = mock(PaymentCallbackService.class);
        PaymentMaintenanceService service = service(mapper, adapter, callbackService);

        PaymentMaintenanceRunResult result = service.runAt(now);

        assertEquals(1, result.candidates());
        assertEquals(1, result.paid());
        assertEquals(0, result.closed());
        verify(callbackService).reconcilePaidOrder(order.id, PaymentProvider.WECHAT_DIRECT, adapter.queryResult);
    }

    @Test
    void expiredUnpaidOrderIsClosedAtProviderAndLocally() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
        PaymentOrder order = pendingOrder(now.minusMinutes(40), now.minusMinutes(10));
        PaymentOrderMapper mapper = mapperWith(order);
        CapturingAdapter adapter = new CapturingAdapter(new PaymentQueryResult(
                order.orderNo,
                null,
                order.amount,
                "NOTPAY"
        ));
        PaymentMaintenanceService service = service(mapper, adapter, mock(PaymentCallbackService.class));

        PaymentMaintenanceRunResult result = service.runAt(now);

        assertEquals(1, result.closed());
        assertEquals(order.orderNo, adapter.closedOrderNo);
        verify(mapper, atLeastOnce()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void providerFailureIsRetriedWithoutClosingOrder() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
        PaymentOrder order = pendingOrder(now.minusMinutes(5), now.plusMinutes(25));
        PaymentOrderMapper mapper = mapperWith(order);
        CapturingAdapter adapter = new CapturingAdapter(null);
        adapter.queryFailure = new IllegalStateException("temporary provider failure");
        PaymentMaintenanceService service = service(mapper, adapter, mock(PaymentCallbackService.class));

        PaymentMaintenanceRunResult result = service.runAt(now);

        assertEquals(1, result.failed());
        assertEquals(null, adapter.closedOrderNo);
    }

    private PaymentMaintenanceService service(
            PaymentOrderMapper mapper,
            PaymentAdapter adapter,
            PaymentCallbackService callbackService
    ) {
        PaymentMaintenanceProperties properties = new PaymentMaintenanceProperties();
        properties.setEnabled(true);
        properties.setQueryAfter(Duration.ofMinutes(1));
        properties.setPendingTimeout(Duration.ofMinutes(30));
        properties.setRetryDelay(Duration.ofMinutes(2));
        return new PaymentMaintenanceService(
                mapper,
                new PaymentAdapterRegistry(List.of(adapter)),
                callbackService,
                mock(OperationLogService.class),
                properties
        );
    }

    @SuppressWarnings("unchecked")
    private PaymentOrderMapper mapperWith(PaymentOrder order) {
        PaymentOrderMapper mapper = mock(PaymentOrderMapper.class);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(order));
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        return mapper;
    }

    private PaymentOrder pendingOrder(LocalDateTime createdAt, LocalDateTime expiresAt) {
        PaymentOrder order = new PaymentOrder();
        order.id = 10L;
        order.tenantId = 1L;
        order.orderNo = "GP202607200001";
        order.provider = PaymentProvider.WECHAT_DIRECT.name();
        order.scene = PaymentScene.PLAN_ORDER.name();
        order.amount = new BigDecimal("0.01");
        order.payStatus = "CREATED";
        order.createdAt = createdAt;
        order.expiresAt = expiresAt;
        return order;
    }

    private static class CapturingAdapter implements PaymentAdapter {
        private final PaymentQueryResult queryResult;
        private RuntimeException queryFailure;
        private String closedOrderNo;

        private CapturingAdapter(PaymentQueryResult queryResult) {
            this.queryResult = queryResult;
        }

        @Override
        public PaymentProvider provider() {
            return PaymentProvider.WECHAT_DIRECT;
        }

        @Override
        public PaymentCreateResult createPayment(PaymentCreateCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaymentCallbackResult verifyAndParseCallback(PaymentCallbackEnvelope envelope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaymentQueryResult queryPayment(String orderNo) {
            if (queryFailure != null) {
                throw queryFailure;
            }
            return queryResult;
        }

        @Override
        public void closePayment(String orderNo) {
            this.closedOrderNo = orderNo;
        }
    }
}
