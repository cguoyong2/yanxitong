package com.yanxitong.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.device.dto.BindConfirmScreenRequest;
import com.yanxitong.device.dto.ConfirmScreenGiftEvent;
import com.yanxitong.device.dto.ConfirmScreenStatusResult;
import com.yanxitong.device.entity.DeviceBind;
import com.yanxitong.device.mapper.DeviceBindMapper;
import com.yanxitong.device.websocket.ConfirmScreenWebSocketHandler;
import com.yanxitong.gift.entity.BroadcastLog;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.gift.mapper.BroadcastLogMapper;
import com.yanxitong.gift.mapper.GiftRecordMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConfirmScreenServiceTests {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void bindCreatesLightweightConfirmScreenBindingByCodeAndBanquet() {
        TenantContext.setTenantId(1L);
        DeviceBindMapper bindMapper = mock(DeviceBindMapper.class);
        ConfirmScreenWebSocketHandler webSocketHandler = mock(ConfirmScreenWebSocketHandler.class);
        when(bindMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(webSocketHandler.onlineSessions(100L)).thenReturn(2);
        ConfirmScreenService service = service(bindMapper, mock(GiftRecordMapper.class), mock(BroadcastLogMapper.class), webSocketHandler);

        ConfirmScreenStatusResult result = service.bind(bindRequest());

        ArgumentCaptor<DeviceBind> captor = ArgumentCaptor.forClass(DeviceBind.class);
        verify(bindMapper).insert(captor.capture());
        DeviceBind bind = captor.getValue();
        assertEquals(1L, bind.tenantId);
        assertEquals(100L, bind.banquetId);
        assertEquals("CONFIRM_SCREEN", bind.deviceType);
        assertEquals("YXT-100", bind.bindCode);
        assertEquals("BOUND", bind.bindStatus);
        assertEquals(100L, result.banquetId());
        assertEquals("YXT-100", result.bindCode());
        assertEquals("BOUND", result.bindStatus());
        assertTrue(result.online());
        assertEquals(2, result.onlineSessions());
    }

    @Test
    void bindUpdatesExistingCodeToNewBanquetWithoutHardwareSn() {
        DeviceBind existing = new DeviceBind();
        existing.id = 9L;
        existing.bindCode = "YXT-100";
        existing.deviceType = "CONFIRM_SCREEN";
        existing.bindStatus = "UNBOUND";
        DeviceBindMapper bindMapper = mock(DeviceBindMapper.class);
        when(bindMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        ConfirmScreenService service = service(bindMapper, mock(GiftRecordMapper.class), mock(BroadcastLogMapper.class), mock(ConfirmScreenWebSocketHandler.class));

        ConfirmScreenStatusResult result = service.bind(bindRequest());

        verify(bindMapper).updateById(existing);
        assertEquals(100L, existing.banquetId);
        assertEquals("BOUND", existing.bindStatus);
        assertEquals("YXT-100", result.bindCode());
    }

    @Test
    void statusReturnsUnboundForUnknownCode() {
        DeviceBindMapper bindMapper = mock(DeviceBindMapper.class);
        when(bindMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        ConfirmScreenService service = service(bindMapper, mock(GiftRecordMapper.class), mock(BroadcastLogMapper.class), mock(ConfirmScreenWebSocketHandler.class));

        ConfirmScreenStatusResult result = service.status("UNKNOWN");

        assertNull(result.banquetId());
        assertEquals("UNKNOWN", result.bindCode());
        assertEquals("UNBOUND", result.bindStatus());
        assertEquals("CONFIRM_SCREEN", result.deviceType());
        assertFalse(result.online());
        assertEquals(0, result.onlineSessions());
    }

    @Test
    void latestEventUsesConfirmScreenBroadcastContentWhenAvailable() {
        GiftRecordMapper giftRecordMapper = mock(GiftRecordMapper.class);
        BroadcastLogMapper broadcastLogMapper = mock(BroadcastLogMapper.class);
        when(giftRecordMapper.selectOne(any(Wrapper.class))).thenReturn(giftRecord());
        when(broadcastLogMapper.selectOne(any(Wrapper.class))).thenReturn(broadcastLog());
        ConfirmScreenService service = service(mock(DeviceBindMapper.class), giftRecordMapper, broadcastLogMapper, mock(ConfirmScreenWebSocketHandler.class));

        ConfirmScreenGiftEvent event = service.latestEvent(100L);

        assertEquals("GIFT_PAID", event.type());
        assertEquals(100L, event.banquetId());
        assertEquals(30L, event.giftRecordId());
        assertEquals("张三", event.guestName());
        assertEquals(new BigDecimal("188.00"), event.amount());
        assertEquals("张三随礼188元", event.message());
        assertEquals(LocalDateTime.of(2026, 7, 7, 18, 0), event.paidAt());
    }

    private ConfirmScreenService service(
            DeviceBindMapper bindMapper,
            GiftRecordMapper giftRecordMapper,
            BroadcastLogMapper broadcastLogMapper,
            ConfirmScreenWebSocketHandler webSocketHandler
    ) {
        return new ConfirmScreenService(
                bindMapper,
                giftRecordMapper,
                broadcastLogMapper,
                webSocketHandler,
                mock(OperationLogService.class)
        );
    }

    private BindConfirmScreenRequest bindRequest() {
        BindConfirmScreenRequest request = new BindConfirmScreenRequest();
        request.banquetId = 100L;
        request.bindCode = "YXT-100";
        return request;
    }

    private GiftRecord giftRecord() {
        GiftRecord giftRecord = new GiftRecord();
        giftRecord.id = 30L;
        giftRecord.banquetId = 100L;
        giftRecord.guestName = "张三";
        giftRecord.amount = new BigDecimal("188.00");
        giftRecord.blessing = "祝福";
        giftRecord.receivedAt = LocalDateTime.of(2026, 7, 7, 18, 0);
        return giftRecord;
    }

    private BroadcastLog broadcastLog() {
        BroadcastLog log = new BroadcastLog();
        log.giftRecordId = 30L;
        log.eventType = "GIFT_PAID";
        log.deviceType = "CONFIRM_SCREEN";
        log.content = "张三随礼188元";
        log.status = "PUSHED";
        return log;
    }
}
