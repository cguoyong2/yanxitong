package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.order.OrderNoGenerator;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import com.yanxitong.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTests {
    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void duplicateClientRequestReturnsExistingPaymentOrder() {
        TenantContext.setTenantId(1L);
        PaymentOrder existing = order("GP_EXISTING");
        existing.payPayload = "{\"prepay\":\"existing\"}";
        PaymentOrderMapper mapper = mock(PaymentOrderMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        CapturingAdapter adapter = new CapturingAdapter();
        PaymentService service = service(mapper, adapter);

        PaymentOrderCreateResult result = service.createOrder(command("gift-request-1"));

        assertEquals(existing, result.order());
        assertEquals("{\"prepay\":\"existing\"}", result.payPayload());
        assertEquals(0, adapter.createCalls);
        verify(mapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    void newClientRequestStoresIdempotencyKey() {
        TenantContext.setTenantId(1L);
        PaymentOrderMapper mapper = mock(PaymentOrderMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            PaymentOrder order = invocation.getArgument(0);
            order.id = 10L;
            return 1;
        }).when(mapper).insert(any(PaymentOrder.class));
        CapturingAdapter adapter = new CapturingAdapter();
        PaymentService service = service(mapper, adapter);

        PaymentOrderCreateResult result = service.createOrder(command(" gift-request-2 "));

        assertEquals("gift-request-2", result.order().clientRequestId);
        assertEquals(1, adapter.createCalls);
        verify(mapper).insert(any(PaymentOrder.class));
    }

    private PaymentService service(PaymentOrderMapper mapper, PaymentAdapter adapter) {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.setDefaultProvider(PaymentProvider.MOCK);
        return new PaymentService(
                mapper,
                new PaymentAdapterRegistry(List.of(adapter)),
                new OrderNoGenerator(),
                mock(OperationLogService.class),
                properties
        );
    }

    private PaymentOrderCreateCommand command(String clientRequestId) {
        return new PaymentOrderCreateCommand(
                100L,
                PaymentScene.ONLINE_GIFT,
                "ONLINE_GIFT",
                new BigDecimal("66.00"),
                "宴席礼金",
                "张三",
                "openid-1",
                "新婚快乐",
                clientRequestId
        );
    }

    private PaymentOrder order(String orderNo) {
        PaymentOrder order = new PaymentOrder();
        order.id = 1L;
        order.tenantId = 1L;
        order.banquetId = 100L;
        order.orderNo = orderNo;
        order.clientRequestId = "gift-request-1";
        order.provider = PaymentProvider.MOCK.name();
        order.scene = PaymentScene.ONLINE_GIFT.name();
        order.entrySource = "ONLINE_GIFT";
        order.amount = new BigDecimal("66.00");
        order.payStatus = "CREATED";
        return order;
    }

    private static class CapturingAdapter implements PaymentAdapter {
        private int createCalls;

        @Override
        public PaymentProvider provider() {
            return PaymentProvider.MOCK;
        }

        @Override
        public PaymentCreateResult createPayment(PaymentCreateCommand command) {
            createCalls++;
            return new PaymentCreateResult(
                    command.orderNo(),
                    "MOCK-" + command.orderNo(),
                    "{\"prepay\":\"new\"}",
                    "prepay-id",
                    LocalDateTime.now().plusMinutes(10)
            );
        }

        @Override
        public PaymentCallbackResult verifyAndParseCallback(PaymentCallbackEnvelope envelope) {
            throw new UnsupportedOperationException();
        }
    }
}
