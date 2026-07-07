package com.yanxitong.payment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanxitong.device.DeviceOrderService;
import com.yanxitong.common.PageResult;
import com.yanxitong.gift.GiftService;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.order.PlanOrderService;
import com.yanxitong.payment.dto.ManualSettlePaymentOrderRequest;
import com.yanxitong.payment.dto.ResolvePaymentCallbackRequest;
import com.yanxitong.payment.entity.PaymentCallbackLog;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentCallbackLogMapper;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import com.yanxitong.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCallbackService {
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentCallbackLogMapper paymentCallbackLogMapper;
    private final PaymentAdapterRegistry paymentAdapterRegistry;
    private final GiftService giftService;
    private final PlanOrderService planOrderService;
    private final DeviceOrderService deviceOrderService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public PaymentCallbackService(
            PaymentOrderMapper paymentOrderMapper,
            PaymentCallbackLogMapper paymentCallbackLogMapper,
            PaymentAdapterRegistry paymentAdapterRegistry,
            GiftService giftService,
            PlanOrderService planOrderService,
            DeviceOrderService deviceOrderService,
            OperationLogService operationLogService,
            ObjectMapper objectMapper
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentCallbackLogMapper = paymentCallbackLogMapper;
        this.paymentAdapterRegistry = paymentAdapterRegistry;
        this.giftService = giftService;
        this.planOrderService = planOrderService;
        this.deviceOrderService = deviceOrderService;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    public PageResult<PaymentOrder> listOrders(String payStatus, String scene, Integer page, Integer pageSize) {
        QueryWrapper<PaymentOrder> countQuery = orderQuery(payStatus, scene);
        long total = paymentOrderMapper.selectCount(countQuery);
        QueryWrapper<PaymentOrder> query = orderQuery(payStatus, scene);
        int normalizedPageSize = PageResult.normalizePageSize(pageSize);
        query.orderByDesc("created_at").last("LIMIT " + PageResult.offset(page, pageSize) + ", " + normalizedPageSize);
        return PageResult.of(paymentOrderMapper.selectList(query), total, page, pageSize);
    }

    private QueryWrapper<PaymentOrder> orderQuery(String payStatus, String scene) {
        QueryWrapper<PaymentOrder> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        if (payStatus != null && !payStatus.isBlank()) {
            query.eq("pay_status", payStatus);
        }
        if (scene != null && !scene.isBlank()) {
            query.eq("scene", scene);
        }
        return query;
    }

    public PageResult<PaymentCallbackLog> listCallbacks(String processStatus, String verifyStatus, Integer page, Integer pageSize) {
        QueryWrapper<PaymentCallbackLog> countQuery = callbackQuery(processStatus, verifyStatus);
        long total = paymentCallbackLogMapper.selectCount(countQuery);
        QueryWrapper<PaymentCallbackLog> query = callbackQuery(processStatus, verifyStatus);
        int normalizedPageSize = PageResult.normalizePageSize(pageSize);
        query.orderByDesc("created_at").last("LIMIT " + PageResult.offset(page, pageSize) + ", " + normalizedPageSize);
        return PageResult.of(paymentCallbackLogMapper.selectList(query), total, page, pageSize);
    }

    private QueryWrapper<PaymentCallbackLog> callbackQuery(String processStatus, String verifyStatus) {
        QueryWrapper<PaymentCallbackLog> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        if (processStatus != null && !processStatus.isBlank()) {
            query.eq("process_status", processStatus);
        }
        if (verifyStatus != null && !verifyStatus.isBlank()) {
            query.eq("verify_status", verifyStatus);
        }
        return query;
    }

    @Transactional
    public PaymentCallbackLog handleProviderCallback(PaymentProvider provider, String rawBody, String signature) {
        return handleProviderCallback(new PaymentCallbackEnvelope(provider, rawBody, Map.of(), signature, null));
    }

    @Transactional
    public PaymentCallbackLog handleProviderCallback(PaymentCallbackEnvelope envelope) {
        PaymentCallbackLog log = new PaymentCallbackLog();
        log.provider = envelope.provider().name();
        log.requestId = envelope.requestId();
        log.headers = serializeHeaders(envelope.headers());
        log.rawBody = envelope.rawBody();
        log.signature = envelope.signature();
        log.verifyStatus = "RECEIVED";
        log.processStatus = "PROCESSING";
        paymentCallbackLogMapper.insert(log);

        try {
            PaymentCallbackResult result = paymentAdapterRegistry.get(envelope.provider()).verifyAndParseCallback(envelope);
            log.orderNo = result.orderNo();
            log.providerTradeNo = result.providerTradeNo();
            log.providerEventId = result.providerEventId();
            log.providerSerialNo = result.providerSerialNo();
            log.eventType = result.eventType();
            log.resourceType = result.resourceType();
            log.decryptedBody = result.decryptedBody();
            log.verifyStatus = "VERIFIED";

            if (hasSuccessfulProviderEvent(log.provider, log.providerEventId, log.id)) {
                log.processStatus = "IGNORED";
                log.errorMessage = "duplicate provider event already processed";
                paymentCallbackLogMapper.updateById(log);
                operationLogService.record(OperationModule.PAYMENT, "CALLBACK_EVENT_DUPLICATE_IGNORED", "payment_callback_log", log.id, "duplicate payment provider event ignored");
                return log;
            }

            if (!result.success()) {
                log.processStatus = "IGNORED";
                log.errorMessage = "callback is not a success payment event";
                paymentCallbackLogMapper.updateById(log);
                return log;
            }

            PaymentOrder order = findOrder(result.orderNo());
            if (order == null) {
                log.processStatus = "FAILED";
                log.errorMessage = "payment order not found";
                paymentCallbackLogMapper.updateById(log);
                operationLogService.record(OperationModule.PAYMENT, "CALLBACK_ORDER_NOT_FOUND", "payment_callback_log", log.id, "payment callback order not found");
                return log;
            }

            log.tenantId = order.tenantId;
            if (result.paidAmount() != null && order.amount.compareTo(result.paidAmount()) != 0) {
                log.processStatus = "FAILED";
                log.errorMessage = "paid amount mismatch";
                paymentCallbackLogMapper.updateById(log);
                operationLogService.record(OperationModule.PAYMENT, "CALLBACK_AMOUNT_MISMATCH", "payment_callback_log", log.id, "payment callback amount mismatch");
                return log;
            }

            if ("PAID".equals(order.payStatus)) {
                if (tradeNoConflicts(order.providerTradeNo, result.providerTradeNo())) {
                    log.processStatus = "FAILED";
                    log.errorMessage = "paid order provider trade no mismatch";
                    paymentCallbackLogMapper.updateById(log);
                    operationLogService.record(OperationModule.PAYMENT, "CALLBACK_TRADE_NO_MISMATCH", "payment_callback_log", log.id, "payment callback trade no mismatch");
                    return log;
                }
                ensureProviderTradeNo(order, result.providerTradeNo());
                fulfill(order);
                log.processStatus = "IGNORED";
                log.errorMessage = "duplicate success callback for paid order";
                paymentCallbackLogMapper.updateById(log);
                operationLogService.record(OperationModule.PAYMENT, "CALLBACK_DUPLICATE_IGNORED", "payment_order", order.id, "duplicate payment callback ignored");
                return log;
            }

            markPaid(order, result.providerTradeNo());
            fulfill(order);
            log.processStatus = "SUCCESS";
            paymentCallbackLogMapper.updateById(log);
            operationLogService.record(OperationModule.PAYMENT, "CALLBACK_SUCCESS", "payment_order", order.id, "payment callback success");
            return log;
        } catch (RuntimeException ex) {
            log.verifyStatus = "FAILED";
            log.processStatus = "FAILED";
            log.errorMessage = ex.getMessage();
            paymentCallbackLogMapper.updateById(log);
            operationLogService.record(OperationModule.PAYMENT, "CALLBACK_FAILED", "payment_callback_log", log.id, "payment callback failed");
            return log;
        }
    }

    private String serializeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException ex) {
            return headers.toString();
        }
    }

    @Transactional
    public GiftRecord mockSuccess(String orderNo) {
        PaymentOrder order = findOrder(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("Payment order not found");
        }
        markPaid(order, order.providerTradeNo);

        PaymentCallbackLog callbackLog = new PaymentCallbackLog();
        callbackLog.tenantId = order.tenantId;
        callbackLog.provider = order.provider;
        callbackLog.orderNo = order.orderNo;
        callbackLog.providerTradeNo = order.providerTradeNo;
        callbackLog.rawBody = "{\"mock\":true}";
        callbackLog.verifyStatus = "VERIFIED";
        callbackLog.processStatus = "SUCCESS";
        paymentCallbackLogMapper.insert(callbackLog);

        GiftRecord giftRecord = fulfill(order);
        operationLogService.record(OperationModule.PAYMENT, "MOCK_PAYMENT_SUCCESS", "payment_order", order.id, "mock gift payment success");
        return giftRecord;
    }

    @Transactional
    public PaymentCallbackLog resolveCallback(Long id, ResolvePaymentCallbackRequest request) {
        PaymentCallbackLog log = paymentCallbackLogMapper.selectById(id);
        if (log == null) {
            throw new IllegalArgumentException("Payment callback log not found");
        }
        String status = request.processStatus == null || request.processStatus.isBlank()
                ? "HANDLED"
                : request.processStatus;
        if (!"HANDLED".equals(status) && !"IGNORED".equals(status)) {
            throw new IllegalArgumentException("Unsupported callback process status");
        }
        log.processStatus = status;
        log.handleRemark = request.handleRemark;
        log.handledAt = LocalDateTime.now();
        paymentCallbackLogMapper.updateById(log);
        operationLogService.record(OperationModule.PAYMENT, "RESOLVE_CALLBACK", "payment_callback_log", log.id, "resolve payment callback");
        return log;
    }

    @Transactional
    public PaymentCallbackLog retryCallback(Long id, ResolvePaymentCallbackRequest request) {
        PaymentCallbackLog original = paymentCallbackLogMapper.selectById(id);
        if (original == null) {
            throw new IllegalArgumentException("Payment callback log not found");
        }
        if (original.rawBody == null || original.rawBody.isBlank()) {
            throw new IllegalArgumentException("Payment callback raw body is empty");
        }
        PaymentProvider provider = PaymentProvider.valueOf(original.provider);
        PaymentCallbackLog retryLog = handleProviderCallback(new PaymentCallbackEnvelope(
                provider,
                original.rawBody,
                deserializeHeaders(original.headers),
                original.signature,
                original.requestId
        ));
        original.processStatus = "HANDLED";
        original.handleRemark = appendRemark(request == null ? null : request.handleRemark, "retried as callback log " + retryLog.id);
        original.handledAt = LocalDateTime.now();
        paymentCallbackLogMapper.updateById(original);
        operationLogService.record(OperationModule.PAYMENT, "RETRY_CALLBACK", "payment_callback_log", retryLog.id, "retry payment callback");
        return retryLog;
    }

    @Transactional
    public GiftRecord compensateFulfillment(Long orderId, ResolvePaymentCallbackRequest request) {
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Payment order not found");
        }
        if (!"PAID".equals(order.payStatus)) {
            throw new IllegalArgumentException("Only paid payment orders can be fulfilled");
        }
        GiftRecord giftRecord = fulfill(order);
        operationLogService.record(OperationModule.PAYMENT, "COMPENSATE_FULFILLMENT", "payment_order", order.id,
                appendRemark(request == null ? null : request.handleRemark, "compensate paid order fulfillment"));
        return giftRecord;
    }

    @Transactional
    public GiftRecord manualSettleOrder(Long orderId, ManualSettlePaymentOrderRequest request) {
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Payment order not found");
        }
        if (request == null || request.providerTradeNo == null || request.providerTradeNo.isBlank()) {
            throw new IllegalArgumentException("Manual settlement provider trade no is required");
        }
        if ("PAID".equals(order.payStatus) && tradeNoConflicts(order.providerTradeNo, request == null ? null : request.providerTradeNo)) {
            throw new IllegalArgumentException("Paid order provider trade no mismatch");
        }
        if (!"PAID".equals(order.payStatus)) {
            order.payStatus = "PAID";
            order.providerTradeNo = request.providerTradeNo;
            order.paidAt = LocalDateTime.now();
            paymentOrderMapper.updateById(order);
        } else {
            ensureProviderTradeNo(order, request.providerTradeNo);
        }
        GiftRecord giftRecord = fulfill(order);
        operationLogService.record(OperationModule.PAYMENT, "MANUAL_SETTLE_ORDER", "payment_order", order.id,
                appendRemark(request == null ? null : request.handleRemark, "manual payment order settlement"));
        return giftRecord;
    }

    private PaymentOrder findOrder(String orderNo) {
        return paymentOrderMapper.selectOne(new QueryWrapper<PaymentOrder>()
                .eq("order_no", orderNo)
                .last("LIMIT 1"));
    }

    private boolean hasSuccessfulProviderEvent(String provider, String providerEventId, Long currentLogId) {
        if (providerEventId == null || providerEventId.isBlank()) {
            return false;
        }
        Long count = paymentCallbackLogMapper.selectCount(new QueryWrapper<PaymentCallbackLog>()
                .eq("provider", provider)
                .eq("provider_event_id", providerEventId)
                .eq("process_status", "SUCCESS")
                .ne(currentLogId != null, "id", currentLogId));
        return count != null && count > 0;
    }

    private void markPaid(PaymentOrder order, String providerTradeNo) {
        if (!"PAID".equals(order.payStatus)) {
            order.payStatus = "PAID";
            order.providerTradeNo = providerTradeNo == null ? order.providerTradeNo : providerTradeNo;
            order.paidAt = LocalDateTime.now();
            paymentOrderMapper.updateById(order);
        }
    }

    private boolean tradeNoConflicts(String existingTradeNo, String callbackTradeNo) {
        return existingTradeNo != null && !existingTradeNo.isBlank()
                && callbackTradeNo != null && !callbackTradeNo.isBlank()
                && !existingTradeNo.equals(callbackTradeNo);
    }

    private void ensureProviderTradeNo(PaymentOrder order, String providerTradeNo) {
        if ((order.providerTradeNo == null || order.providerTradeNo.isBlank())
                && providerTradeNo != null && !providerTradeNo.isBlank()) {
            order.providerTradeNo = providerTradeNo;
            paymentOrderMapper.updateById(order);
        }
    }

    private GiftRecord fulfill(PaymentOrder order) {
        if (PaymentScene.ONLINE_GIFT.name().equals(order.scene)) {
            return giftService.fulfillPaidPaymentOrder(order);
        }
        if (PaymentScene.PLAN_ORDER.name().equals(order.scene)) {
            planOrderService.fulfillPaidPaymentOrder(order);
            return null;
        }
        if (PaymentScene.DEVICE_ORDER.name().equals(order.scene)) {
            deviceOrderService.fulfillPaidPaymentOrder(order);
            return null;
        }
        throw new IllegalArgumentException("Unsupported payment scene: " + order.scene);
    }

    private Map<String, String> deserializeHeaders(String headers) {
        if (headers == null || headers.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(headers, STRING_MAP);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String appendRemark(String remark, String action) {
        if (remark == null || remark.isBlank()) {
            return action;
        }
        return remark + " | " + action;
    }
}
