package com.yanxitong.gift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.device.ConfirmScreenEventPublisher;
import com.yanxitong.favor.FavorService;
import com.yanxitong.gift.entity.BroadcastLog;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.gift.mapper.BroadcastLogMapper;
import com.yanxitong.gift.mapper.GiftRecordMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.payment.PaymentService;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import com.yanxitong.theme.ThemeResolutionService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GiftServiceTests {
    @Test
    void duplicatePaymentFulfillmentReturnsExistingGiftRecord() {
        GiftRecord existing = new GiftRecord();
        existing.id = 30L;
        existing.paymentOrderId = 10L;
        existing.amount = new BigDecimal("88.00");
        GiftRecordMapper giftRecordMapper = mock(GiftRecordMapper.class);
        when(giftRecordMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        FavorService favorService = mock(FavorService.class);
        BroadcastLogMapper broadcastLogMapper = mock(BroadcastLogMapper.class);
        GiftService service = service(giftRecordMapper, broadcastLogMapper, favorService);

        GiftRecord result = service.fulfillPaidPaymentOrder(paymentOrder());

        assertEquals(existing, result);
        verify(giftRecordMapper, never()).insert(any(GiftRecord.class));
        verify(favorService, never()).recordReceivedGift(any());
        verify(broadcastLogMapper, never()).insert(any(BroadcastLog.class));
    }

    private GiftService service(
            GiftRecordMapper giftRecordMapper,
            BroadcastLogMapper broadcastLogMapper,
            FavorService favorService
    ) {
        return new GiftService(
                mock(PaymentOrderMapper.class),
                giftRecordMapper,
                broadcastLogMapper,
                mock(BanquetMapper.class),
                mock(PaymentService.class),
                favorService,
                mock(ThemeResolutionService.class),
                mock(ConfirmScreenEventPublisher.class),
                mock(OperationLogService.class)
        );
    }

    private PaymentOrder paymentOrder() {
        PaymentOrder order = new PaymentOrder();
        order.id = 10L;
        order.banquetId = 100L;
        order.scene = "ONLINE_GIFT";
        order.entrySource = "ONLINE_GIFT";
        order.payerName = "张三";
        order.amount = new BigDecimal("88.00");
        return order;
    }
}
