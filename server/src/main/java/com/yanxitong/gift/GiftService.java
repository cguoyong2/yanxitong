package com.yanxitong.gift;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.common.PageResult;
import com.yanxitong.device.ConfirmScreenEventPublisher;
import com.yanxitong.device.dto.ConfirmScreenGiftEvent;
import com.yanxitong.favor.FavorService;
import com.yanxitong.gift.dto.CreateGiftPaymentRequest;
import com.yanxitong.gift.dto.GiftPaymentOrderResult;
import com.yanxitong.gift.dto.GiftSummaryResult;
import com.yanxitong.gift.dto.OfflineGiftRequest;
import com.yanxitong.gift.entity.BroadcastLog;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.gift.mapper.BroadcastLogMapper;
import com.yanxitong.gift.mapper.GiftRecordMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.payment.PaymentOrderCreateCommand;
import com.yanxitong.payment.PaymentOrderCreateResult;
import com.yanxitong.payment.PaymentService;
import com.yanxitong.payment.PaymentScene;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import com.yanxitong.tenant.TenantContext;
import com.yanxitong.theme.ThemeResolutionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GiftService {
    private final PaymentOrderMapper paymentOrderMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final BroadcastLogMapper broadcastLogMapper;
    private final BanquetMapper banquetMapper;
    private final PaymentService paymentService;
    private final FavorService favorService;
    private final ThemeResolutionService themeResolutionService;
    private final ConfirmScreenEventPublisher confirmScreenEventPublisher;
    private final OperationLogService operationLogService;

    public GiftService(
            PaymentOrderMapper paymentOrderMapper,
            GiftRecordMapper giftRecordMapper,
            BroadcastLogMapper broadcastLogMapper,
            BanquetMapper banquetMapper,
            PaymentService paymentService,
            FavorService favorService,
            ThemeResolutionService themeResolutionService,
            ConfirmScreenEventPublisher confirmScreenEventPublisher,
            OperationLogService operationLogService
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.giftRecordMapper = giftRecordMapper;
        this.broadcastLogMapper = broadcastLogMapper;
        this.banquetMapper = banquetMapper;
        this.paymentService = paymentService;
        this.favorService = favorService;
        this.themeResolutionService = themeResolutionService;
        this.confirmScreenEventPublisher = confirmScreenEventPublisher;
        this.operationLogService = operationLogService;
    }

    public GiftPaymentOrderResult createPaymentOrder(CreateGiftPaymentRequest request) {
        if (!"ONLINE_GIFT".equals(request.entrySource) && !"ONSITE_QR".equals(request.entrySource)) {
            throw new IllegalArgumentException("Unsupported gift payment entry source");
        }
        PaymentOrderCreateResult result = paymentService.createOrder(new PaymentOrderCreateCommand(
                request.banquetId,
                PaymentScene.ONLINE_GIFT,
                request.entrySource,
                request.amount,
                "宴席礼金",
                request.guestName,
                request.payerOpenId,
                request.blessing,
                request.clientRequestId
        ));
        return GiftPaymentOrderResult.from(result);
    }

    @Transactional
    public GiftRecord fulfillPaidPaymentOrder(PaymentOrder order) {
        if (!PaymentScene.ONLINE_GIFT.name().equals(order.scene)) {
            throw new IllegalArgumentException("Unsupported gift payment scene");
        }
        GiftRecord existing = giftRecordMapper.selectOne(new QueryWrapper<GiftRecord>()
                .eq("payment_order_id", order.id)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        return createGiftRecord(order.banquetId, order.id, order.entrySource, order.payerName, order.amount, order.blessing);
    }

    @Transactional
    public GiftRecord offlineGift(OfflineGiftRequest request) {
        normalize(request);
        GiftRecord giftRecord = createGiftRecord(request.banquetId, null, "CASH", request.guestName, request.amount, request.blessing);
        operationLogService.record(OperationModule.GIFT, "OFFLINE_GIFT", "gift_record", giftRecord.id, "offline cash gift");
        return giftRecord;
    }

    public List<GiftRecord> list(Long banquetId, String source, String keyword) {
        QueryWrapper<GiftRecord> query = new QueryWrapper<GiftRecord>()
                .eq("banquet_id", banquetId);
        if (source != null && !source.isBlank()) {
            query.eq("gift_source", source);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like("guest_name", keyword);
        }
        query.orderByDesc("received_at");
        return giftRecordMapper.selectList(query);
    }

    public PageResult<GiftRecord> adminPage(Long banquetId, String source, String keyword, Integer page, Integer pageSize) {
        QueryWrapper<GiftRecord> countQuery = adminQuery(banquetId, source, keyword);
        long total = giftRecordMapper.selectCount(countQuery);
        QueryWrapper<GiftRecord> query = adminQuery(banquetId, source, keyword);
        int normalizedPageSize = PageResult.normalizePageSize(pageSize);
        query.orderByDesc("received_at").last("LIMIT " + PageResult.offset(page, pageSize) + ", " + normalizedPageSize);
        return PageResult.of(giftRecordMapper.selectList(query), total, page, pageSize);
    }

    private QueryWrapper<GiftRecord> adminQuery(Long banquetId, String source, String keyword) {
        QueryWrapper<GiftRecord> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        if (banquetId != null) {
            query.eq("banquet_id", banquetId);
        }
        if (source != null && !source.isBlank()) {
            query.eq("gift_source", source);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like("guest_name", keyword);
        }
        return query;
    }

    public GiftSummaryResult summary(Long banquetId) {
        List<GiftRecord> records = list(banquetId, null, null);
        BigDecimal totalAmount = records.stream()
                .map(record -> record.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> sourceCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> sourceAmounts = new LinkedHashMap<>();
        for (GiftRecord record : records) {
            sourceCounts.merge(record.giftSource, 1L, Long::sum);
            sourceAmounts.merge(record.giftSource, record.amount, BigDecimal::add);
        }
        return new GiftSummaryResult(banquetId, records.size(), totalAmount, sourceCounts, sourceAmounts);
    }

    private GiftRecord createGiftRecord(Long banquetId, Long paymentOrderId, String source, String guestName, java.math.BigDecimal amount, String blessing) {
        Banquet banquet = banquetMapper.selectById(banquetId);
        GiftRecord giftRecord = new GiftRecord();
        giftRecord.tenantId = banquet == null ? TenantContext.getTenantId() : banquet.tenantId;
        giftRecord.banquetId = banquetId;
        giftRecord.paymentOrderId = paymentOrderId;
        giftRecord.giftSource = source;
        giftRecord.guestName = guestName;
        giftRecord.amount = amount;
        giftRecord.blessing = blessing;
        giftRecord.receivedAt = LocalDateTime.now();
        giftRecordMapper.insert(giftRecord);
        favorService.recordReceivedGift(giftRecord);
        String speakerText = createBroadcastLog(giftRecord);
        int pushedSessions = confirmScreenEventPublisher.publishGiftPaid(new ConfirmScreenGiftEvent(
                "GIFT_PAID",
                giftRecord.banquetId,
                giftRecord.id,
                giftRecord.guestName,
                giftRecord.amount,
                speakerText,
                giftRecord.receivedAt
        ));
        createConfirmScreenLog(giftRecord, speakerText, pushedSessions);
        return giftRecord;
    }

    private void normalize(OfflineGiftRequest request) {
        request.guestName = trimToNull(request.guestName);
        request.blessing = trimToNull(request.blessing);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String createBroadcastLog(GiftRecord giftRecord) {
        Banquet banquet = banquetMapper.selectById(giftRecord.banquetId);
        String content = "收到一份宴席心意";
        if (banquet != null) {
            content = themeResolutionService
                    .resolveGiftSuccess(banquet.customCopywriting, banquet.themeCode, banquet.eventTypeCode)
                    .speakerText();
        }
        BroadcastLog log = new BroadcastLog();
        log.tenantId = giftRecord.tenantId;
        log.banquetId = giftRecord.banquetId;
        log.giftRecordId = giftRecord.id;
        log.deviceType = "CLOUD_SPEAKER";
        log.eventType = "GIFT_PAID";
        log.content = content;
        log.status = "SIMULATED";
        broadcastLogMapper.insert(log);
        return content;
    }

    private void createConfirmScreenLog(GiftRecord giftRecord, String content, int pushedSessions) {
        BroadcastLog log = new BroadcastLog();
        log.tenantId = giftRecord.tenantId;
        log.banquetId = giftRecord.banquetId;
        log.giftRecordId = giftRecord.id;
        log.deviceType = "CONFIRM_SCREEN";
        log.eventType = "GIFT_PAID";
        log.content = content;
        log.status = pushedSessions > 0 ? "PUSHED" : "OFFLINE";
        broadcastLogMapper.insert(log);
    }
}
