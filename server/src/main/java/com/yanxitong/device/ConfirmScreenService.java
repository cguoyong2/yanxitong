package com.yanxitong.device;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.tenant.TenantContext;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class ConfirmScreenService {
    private final DeviceBindMapper deviceBindMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final BroadcastLogMapper broadcastLogMapper;
    private final ConfirmScreenWebSocketHandler webSocketHandler;
    private final OperationLogService operationLogService;

    public ConfirmScreenService(
            DeviceBindMapper deviceBindMapper,
            GiftRecordMapper giftRecordMapper,
            BroadcastLogMapper broadcastLogMapper,
            ConfirmScreenWebSocketHandler webSocketHandler,
            OperationLogService operationLogService
    ) {
        this.deviceBindMapper = deviceBindMapper;
        this.giftRecordMapper = giftRecordMapper;
        this.broadcastLogMapper = broadcastLogMapper;
        this.webSocketHandler = webSocketHandler;
        this.operationLogService = operationLogService;
    }

    public ConfirmScreenStatusResult bind(BindConfirmScreenRequest request) {
        DeviceBind bind = deviceBindMapper.selectOne(new QueryWrapper<DeviceBind>()
                .eq("bind_code", request.bindCode)
                .last("LIMIT 1"));
        if (bind == null) {
            bind = new DeviceBind();
            bind.tenantId = TenantContext.getTenantId();
            bind.banquetId = request.banquetId;
            bind.deviceType = "CONFIRM_SCREEN";
            bind.bindCode = request.bindCode;
        }
        bind.banquetId = request.banquetId;
        bind.bindStatus = "BOUND";
        bind.boundAt = LocalDateTime.now();
        if (bind.id == null) {
            deviceBindMapper.insert(bind);
        } else {
            deviceBindMapper.updateById(bind);
        }
        operationLogService.record(OperationModule.DEVICE, "BIND_CONFIRM_SCREEN", "device_bind", bind.id, "bind confirm screen");
        return toStatus(bind);
    }

    public ConfirmScreenStatusResult status(String bindCode) {
        DeviceBind bind = deviceBindMapper.selectOne(new QueryWrapper<DeviceBind>()
                .eq("bind_code", bindCode)
                .last("LIMIT 1"));
        if (bind == null) {
            return new ConfirmScreenStatusResult(null, bindCode, "UNBOUND", "CONFIRM_SCREEN", false, 0);
        }
        return toStatus(bind);
    }

    public ConfirmScreenGiftEvent latestEvent(Long banquetId) {
        GiftRecord giftRecord = giftRecordMapper.selectOne(new QueryWrapper<GiftRecord>()
                .eq("banquet_id", banquetId)
                .orderByDesc("received_at")
                .last("LIMIT 1"));
        if (giftRecord == null) {
            return null;
        }
        BroadcastLog log = broadcastLogMapper.selectOne(new QueryWrapper<BroadcastLog>()
                .eq("gift_record_id", giftRecord.id)
                .eq("event_type", "GIFT_PAID")
                .orderByDesc("created_at")
                .last("LIMIT 1"));
        return new ConfirmScreenGiftEvent(
                "GIFT_PAID",
                giftRecord.banquetId,
                giftRecord.id,
                giftRecord.guestName,
                giftRecord.amount,
                log == null ? giftRecord.blessing : log.content,
                giftRecord.receivedAt
        );
    }

    private ConfirmScreenStatusResult toStatus(DeviceBind bind) {
        int onlineSessions = webSocketHandler.onlineSessions(bind.banquetId);
        return new ConfirmScreenStatusResult(
                bind.banquetId,
                bind.bindCode,
                bind.bindStatus,
                bind.deviceType,
                onlineSessions > 0,
                onlineSessions
        );
    }
}
