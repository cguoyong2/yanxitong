package com.yanxitong.gift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.device.ConfirmScreenEventPublisher;
import com.yanxitong.device.dto.ConfirmScreenGiftEvent;
import com.yanxitong.favor.FavorService;
import com.yanxitong.gift.dto.OfflineGiftRequest;
import com.yanxitong.gift.entity.BroadcastLog;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.gift.mapper.BroadcastLogMapper;
import com.yanxitong.gift.mapper.GiftRecordMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.payment.PaymentService;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import com.yanxitong.theme.ThemeResolutionService;
import com.yanxitong.theme.dto.ResolvedCopywriting;
import com.yanxitong.theme.CopywritingPriority;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    @Test
    void offlineGiftNormalizesGuestAndBlessingBeforeLedgerSync() {
        GiftRecordMapper giftRecordMapper = mock(GiftRecordMapper.class);
        BroadcastLogMapper broadcastLogMapper = mock(BroadcastLogMapper.class);
        FavorService favorService = mock(FavorService.class);
        GiftService service = service(giftRecordMapper, broadcastLogMapper, favorService);

        GiftRecord result = service.offlineGift(offlineRequest(" 现金来宾 ", " 现金备注 "));

        ArgumentCaptor<GiftRecord> captor = ArgumentCaptor.forClass(GiftRecord.class);
        verify(giftRecordMapper).insert(captor.capture());
        GiftRecord inserted = captor.getValue();
        assertEquals("现金来宾", inserted.guestName);
        assertEquals("现金备注", inserted.blessing);
        assertEquals("现金来宾", result.guestName);
        assertEquals("现金备注", result.blessing);
        verify(favorService).recordReceivedGift(inserted);
    }

    @Test
    void offlineGiftWritesSpeakerAndConfirmScreenLogsAfterGiftSync() {
        GiftRecordMapper giftRecordMapper = mock(GiftRecordMapper.class);
        BroadcastLogMapper broadcastLogMapper = mock(BroadcastLogMapper.class);
        FavorService favorService = mock(FavorService.class);
        BanquetMapper banquetMapper = mock(BanquetMapper.class);
        ThemeResolutionService themeResolutionService = mock(ThemeResolutionService.class);
        ConfirmScreenEventPublisher publisher = mock(ConfirmScreenEventPublisher.class);
        doAnswer(invocation -> {
            GiftRecord record = invocation.getArgument(0);
            record.id = 30L;
            return 1;
        }).when(giftRecordMapper).insert(any(GiftRecord.class));
        when(banquetMapper.selectById(100L)).thenReturn(banquet());
        when(themeResolutionService.resolveGiftSuccess("{}", "wedding_red_gold", "WEDDING"))
                .thenReturn(new ResolvedCopywriting("心意已收到", "感谢祝福", "张三随礼成功", CopywritingPriority.BANQUET_CUSTOM));
        when(publisher.publishGiftPaid(any(ConfirmScreenGiftEvent.class))).thenReturn(1);
        GiftService service = service(giftRecordMapper, broadcastLogMapper, favorService, banquetMapper, themeResolutionService, publisher);

        GiftRecord result = service.offlineGift(offlineRequest(" 张三 ", " 百年好合 "));

        ArgumentCaptor<BroadcastLog> logCaptor = ArgumentCaptor.forClass(BroadcastLog.class);
        verify(broadcastLogMapper, times(2)).insert(logCaptor.capture());
        List<BroadcastLog> logs = logCaptor.getAllValues();
        BroadcastLog speakerLog = logs.get(0);
        BroadcastLog screenLog = logs.get(1);
        assertEquals(30L, result.id);
        assertEquals("CLOUD_SPEAKER", speakerLog.deviceType);
        assertEquals("GIFT_PAID", speakerLog.eventType);
        assertEquals("张三随礼成功", speakerLog.content);
        assertEquals("SIMULATED", speakerLog.status);
        assertEquals("CONFIRM_SCREEN", screenLog.deviceType);
        assertEquals("GIFT_PAID", screenLog.eventType);
        assertEquals("张三随礼成功", screenLog.content);
        assertEquals("PUSHED", screenLog.status);

        ArgumentCaptor<ConfirmScreenGiftEvent> eventCaptor = ArgumentCaptor.forClass(ConfirmScreenGiftEvent.class);
        verify(publisher).publishGiftPaid(eventCaptor.capture());
        ConfirmScreenGiftEvent event = eventCaptor.getValue();
        assertEquals("GIFT_PAID", event.type());
        assertEquals(100L, event.banquetId());
        assertEquals(30L, event.giftRecordId());
        assertEquals("张三", event.guestName());
        assertEquals(new BigDecimal("88.00"), event.amount());
        assertEquals("张三随礼成功", event.message());
    }

    private GiftService service(
            GiftRecordMapper giftRecordMapper,
            BroadcastLogMapper broadcastLogMapper,
            FavorService favorService
    ) {
        return service(
                giftRecordMapper,
                broadcastLogMapper,
                favorService,
                mock(BanquetMapper.class),
                mock(ThemeResolutionService.class),
                mock(ConfirmScreenEventPublisher.class)
        );
    }

    private GiftService service(
            GiftRecordMapper giftRecordMapper,
            BroadcastLogMapper broadcastLogMapper,
            FavorService favorService,
            BanquetMapper banquetMapper,
            ThemeResolutionService themeResolutionService,
            ConfirmScreenEventPublisher publisher
    ) {
        return new GiftService(
                mock(PaymentOrderMapper.class),
                giftRecordMapper,
                broadcastLogMapper,
                banquetMapper,
                mock(PaymentService.class),
                favorService,
                themeResolutionService,
                publisher,
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

    private OfflineGiftRequest offlineRequest(String guestName, String blessing) {
        OfflineGiftRequest request = new OfflineGiftRequest();
        request.banquetId = 100L;
        request.guestName = guestName;
        request.amount = new BigDecimal("88.00");
        request.blessing = blessing;
        return request;
    }

    private Banquet banquet() {
        Banquet banquet = new Banquet();
        banquet.id = 100L;
        banquet.eventTypeCode = "WEDDING";
        banquet.themeCode = "wedding_red_gold";
        banquet.customCopywriting = "{}";
        return banquet;
    }
}
