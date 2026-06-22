package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanxitong.gift.GiftService;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.payment.entity.PaymentCallbackLog;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentCallbackLogMapper;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentCallbackServiceTests {
    @Test
    void duplicateProviderEventIsIgnoredBeforeOrderFulfillment() {
        PaymentOrderMapper orderMapper = mock(PaymentOrderMapper.class);
        PaymentCallbackLogMapper callbackLogMapper = callbackLogMapper(1L);
        when(callbackLogMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        GiftService giftService = mock(GiftService.class);
        PaymentCallbackService service = service(
                orderMapper,
                callbackLogMapper,
                giftService,
                callbackResult("GP001", "WX001", true, "event-1")
        );

        PaymentCallbackLog log = service.handleProviderCallback(envelope());

        assertEquals("IGNORED", log.processStatus);
        assertEquals("duplicate provider event already processed", log.errorMessage);
        verify(orderMapper, never()).selectOne(any(Wrapper.class));
        verify(giftService, never()).fulfillPaidPaymentOrder(any());
    }

    @Test
    void duplicateSuccessCallbackForPaidOrderDoesNotMarkSuccessAgain() {
        PaymentOrderMapper orderMapper = mock(PaymentOrderMapper.class);
        PaymentOrder paidOrder = paidOrder("GP001", "WX001");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(paidOrder);
        PaymentCallbackLogMapper callbackLogMapper = callbackLogMapper(2L);
        when(callbackLogMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        GiftService giftService = mock(GiftService.class);
        PaymentCallbackService service = service(
                orderMapper,
                callbackLogMapper,
                giftService,
                callbackResult("GP001", "WX001", true, "event-2")
        );

        PaymentCallbackLog log = service.handleProviderCallback(envelope());

        assertEquals("IGNORED", log.processStatus);
        assertEquals("duplicate success callback for paid order", log.errorMessage);
        verify(giftService).fulfillPaidPaymentOrder(paidOrder);
        verify(orderMapper, never()).updateById(any(PaymentOrder.class));
    }

    @Test
    void paidOrderWithDifferentTradeNoFailsDuplicateCallback() {
        PaymentOrderMapper orderMapper = mock(PaymentOrderMapper.class);
        PaymentOrder paidOrder = paidOrder("GP001", "WX001");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(paidOrder);
        PaymentCallbackLogMapper callbackLogMapper = callbackLogMapper(3L);
        when(callbackLogMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        GiftService giftService = mock(GiftService.class);
        PaymentCallbackService service = service(
                orderMapper,
                callbackLogMapper,
                giftService,
                callbackResult("GP001", "WX-DIFFERENT", true, "event-3")
        );

        PaymentCallbackLog log = service.handleProviderCallback(envelope());

        assertEquals("FAILED", log.processStatus);
        assertEquals("paid order provider trade no mismatch", log.errorMessage);
        verify(giftService, never()).fulfillPaidPaymentOrder(any());
    }

    @Test
    void amountMismatchFailsBeforeFulfillment() {
        PaymentOrderMapper orderMapper = mock(PaymentOrderMapper.class);
        PaymentOrder order = unpaidOrder("GP001");
        order.amount = new BigDecimal("13.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        PaymentCallbackLogMapper callbackLogMapper = callbackLogMapper(4L);
        when(callbackLogMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        GiftService giftService = mock(GiftService.class);
        PaymentCallbackService service = service(
                orderMapper,
                callbackLogMapper,
                giftService,
                callbackResult("GP001", "WX001", true, "event-4")
        );

        PaymentCallbackLog log = service.handleProviderCallback(envelope());

        assertEquals("FAILED", log.processStatus);
        assertEquals("paid amount mismatch", log.errorMessage);
        verify(orderMapper, never()).updateById(any(PaymentOrder.class));
        verify(giftService, never()).fulfillPaidPaymentOrder(any());
    }

    @Test
    void invalidSignatureOrParseFailureIsRecordedAsVerificationFailure() {
        PaymentOrderMapper orderMapper = mock(PaymentOrderMapper.class);
        PaymentCallbackLogMapper callbackLogMapper = callbackLogMapper(5L);
        GiftService giftService = mock(GiftService.class);
        PaymentCallbackService service = serviceWithThrowingAdapter(
                orderMapper,
                callbackLogMapper,
                giftService,
                new IllegalArgumentException("mock callback signature verification failed")
        );

        PaymentCallbackLog log = service.handleProviderCallback(envelope());

        assertEquals("FAILED", log.verifyStatus);
        assertEquals("FAILED", log.processStatus);
        assertEquals("mock callback signature verification failed", log.errorMessage);
        verify(orderMapper, never()).selectOne(any(Wrapper.class));
        verify(giftService, never()).fulfillPaidPaymentOrder(any());
    }

    @Test
    void nonSuccessTradeStateIsIgnoredBeforeOrderLookup() {
        PaymentOrderMapper orderMapper = mock(PaymentOrderMapper.class);
        PaymentCallbackLogMapper callbackLogMapper = callbackLogMapper(6L);
        when(callbackLogMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        GiftService giftService = mock(GiftService.class);
        PaymentCallbackService service = service(
                orderMapper,
                callbackLogMapper,
                giftService,
                callbackResult("GP001", "WX001", false, "event-6")
        );

        PaymentCallbackLog log = service.handleProviderCallback(envelope());

        assertEquals("VERIFIED", log.verifyStatus);
        assertEquals("IGNORED", log.processStatus);
        assertEquals("callback is not a success payment event", log.errorMessage);
        verify(orderMapper, never()).selectOne(any(Wrapper.class));
        verify(giftService, never()).fulfillPaidPaymentOrder(any());
    }

    private PaymentCallbackService service(
            PaymentOrderMapper orderMapper,
            PaymentCallbackLogMapper callbackLogMapper,
            GiftService giftService,
            PaymentCallbackResult callbackResult
    ) {
        PaymentAdapter adapter = new PaymentAdapter() {
            @Override
            public PaymentProvider provider() {
                return PaymentProvider.MOCK;
            }

            @Override
            public PaymentCreateResult createPayment(PaymentCreateCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PaymentCallbackResult verifyAndParseCallback(PaymentCallbackEnvelope envelope) {
                return callbackResult;
            }
        };
        return new PaymentCallbackService(
                orderMapper,
                callbackLogMapper,
                new PaymentAdapterRegistry(List.of(adapter)),
                giftService,
                mock(OperationLogService.class),
                new ObjectMapper()
        );
    }

    private PaymentCallbackService serviceWithThrowingAdapter(
            PaymentOrderMapper orderMapper,
            PaymentCallbackLogMapper callbackLogMapper,
            GiftService giftService,
            RuntimeException failure
    ) {
        PaymentAdapter adapter = new PaymentAdapter() {
            @Override
            public PaymentProvider provider() {
                return PaymentProvider.MOCK;
            }

            @Override
            public PaymentCreateResult createPayment(PaymentCreateCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PaymentCallbackResult verifyAndParseCallback(PaymentCallbackEnvelope envelope) {
                throw failure;
            }
        };
        return new PaymentCallbackService(
                orderMapper,
                callbackLogMapper,
                new PaymentAdapterRegistry(List.of(adapter)),
                giftService,
                mock(OperationLogService.class),
                new ObjectMapper()
        );
    }

    private PaymentCallbackLogMapper callbackLogMapper(Long id) {
        PaymentCallbackLogMapper mapper = mock(PaymentCallbackLogMapper.class);
        doAnswer(invocation -> {
            PaymentCallbackLog log = invocation.getArgument(0);
            log.id = id;
            return 1;
        }).when(mapper).insert(any(PaymentCallbackLog.class));
        doAnswer(invocation -> 1).when(mapper).updateById(any(PaymentCallbackLog.class));
        return mapper;
    }

    private PaymentCallbackResult callbackResult(String orderNo, String tradeNo, boolean success, String eventId) {
        return new PaymentCallbackResult(
                orderNo,
                tradeNo,
                new BigDecimal("12.34"),
                success,
                eventId,
                "SERIAL",
                "TRANSACTION.SUCCESS",
                "encrypt-resource",
                "{}"
        );
    }

    private PaymentOrder paidOrder(String orderNo, String tradeNo) {
        PaymentOrder order = new PaymentOrder();
        order.id = 10L;
        order.orderNo = orderNo;
        order.provider = PaymentProvider.MOCK.name();
        order.scene = PaymentScene.ONLINE_GIFT.name();
        order.amount = new BigDecimal("12.34");
        order.payStatus = "PAID";
        order.providerTradeNo = tradeNo;
        return order;
    }

    private PaymentOrder unpaidOrder(String orderNo) {
        PaymentOrder order = paidOrder(orderNo, null);
        order.payStatus = "CREATED";
        return order;
    }

    private PaymentCallbackEnvelope envelope() {
        return new PaymentCallbackEnvelope(PaymentProvider.MOCK, "{}", Map.of(), "signature", "request-id");
    }
}
