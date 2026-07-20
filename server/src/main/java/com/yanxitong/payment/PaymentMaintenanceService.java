package com.yanxitong.payment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import com.yanxitong.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PaymentMaintenanceService {
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentAdapterRegistry paymentAdapterRegistry;
    private final PaymentCallbackService paymentCallbackService;
    private final OperationLogService operationLogService;
    private final PaymentMaintenanceProperties properties;

    public PaymentMaintenanceService(
            PaymentOrderMapper paymentOrderMapper,
            PaymentAdapterRegistry paymentAdapterRegistry,
            PaymentCallbackService paymentCallbackService,
            OperationLogService operationLogService,
            PaymentMaintenanceProperties properties
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentAdapterRegistry = paymentAdapterRegistry;
        this.paymentCallbackService = paymentCallbackService;
        this.operationLogService = operationLogService;
        this.properties = properties;
    }

    public PaymentMaintenanceRunResult runOnce() {
        if (!properties.isEnabled()) {
            return PaymentMaintenanceRunResult.skippedRun();
        }
        return runAt(LocalDateTime.now());
    }

    PaymentMaintenanceRunResult runAt(LocalDateTime now) {
        List<PaymentOrder> candidates = paymentOrderMapper.selectList(new QueryWrapper<PaymentOrder>()
                .eq("pay_status", "CREATED")
                .ne("provider", PaymentProvider.MOCK.name())
                .le("created_at", now.minus(properties.getQueryAfter()))
                .and(wrapper -> wrapper.isNull("next_query_at").or().le("next_query_at", now))
                .orderByAsc("next_query_at", "created_at")
                .last("LIMIT " + properties.getBatchSize()));

        int paid = 0;
        int closed = 0;
        int pending = 0;
        int failed = 0;
        for (PaymentOrder order : candidates) {
            TenantContext.setTenantId(order.tenantId);
            try {
                ReconcileOutcome outcome = reconcile(order, now);
                switch (outcome) {
                    case PAID -> paid++;
                    case CLOSED -> closed++;
                    case PENDING -> pending++;
                    case FAILED -> failed++;
                }
            } finally {
                TenantContext.clear();
            }
        }
        return new PaymentMaintenanceRunResult(candidates.size(), paid, closed, pending, failed, false);
    }

    private ReconcileOutcome reconcile(PaymentOrder order, LocalDateTime now) {
        try {
            PaymentProvider provider = PaymentProvider.valueOf(order.provider);
            PaymentAdapter adapter = paymentAdapterRegistry.get(provider);
            PaymentQueryResult result = adapter.queryPayment(order.orderNo);
            requireMatchingOrder(order, result);
            recordQueryResult(order, result, now);

            if (result.paid()) {
                paymentCallbackService.reconcilePaidOrder(order.id, provider, result);
                return ReconcileOutcome.PAID;
            }
            if (result.terminalUnpaid()) {
                return markClosed(order, now, "PROVIDER_" + result.providerStatus(), result.providerStatus())
                        ? ReconcileOutcome.CLOSED
                        : ReconcileOutcome.PENDING;
            }
            if (isExpired(order, now)) {
                return closeExpiredOrder(order, provider, adapter, now);
            }

            scheduleNextQuery(order, now, null);
            return ReconcileOutcome.PENDING;
        } catch (RuntimeException ex) {
            recordFailure(order, now, ex);
            return ReconcileOutcome.FAILED;
        }
    }

    private ReconcileOutcome closeExpiredOrder(
            PaymentOrder order,
            PaymentProvider provider,
            PaymentAdapter adapter,
            LocalDateTime now
    ) {
        try {
            adapter.closePayment(order.orderNo);
            return markClosed(order, now, "PAYMENT_TIMEOUT", "CLOSED")
                    ? ReconcileOutcome.CLOSED
                    : ReconcileOutcome.PENDING;
        } catch (RuntimeException closeFailure) {
            // A payment may finish between the query and close calls. Re-query before retrying later.
            PaymentQueryResult latest = adapter.queryPayment(order.orderNo);
            requireMatchingOrder(order, latest);
            recordQueryResult(order, latest, now);
            if (latest.paid()) {
                paymentCallbackService.reconcilePaidOrder(order.id, provider, latest);
                return ReconcileOutcome.PAID;
            }
            if (latest.terminalUnpaid()) {
                return markClosed(order, now, "PROVIDER_" + latest.providerStatus(), latest.providerStatus())
                        ? ReconcileOutcome.CLOSED
                        : ReconcileOutcome.PENDING;
            }
            throw closeFailure;
        }
    }

    private void requireMatchingOrder(PaymentOrder order, PaymentQueryResult result) {
        if (result == null || result.orderNo() == null || !result.orderNo().equals(order.orderNo)) {
            throw new IllegalArgumentException("Provider query order no mismatch");
        }
        if (result.providerStatus() == null || result.providerStatus().isBlank()) {
            throw new IllegalArgumentException("Provider query status is empty");
        }
    }

    private boolean isExpired(PaymentOrder order, LocalDateTime now) {
        LocalDateTime expiresAt = order.expiresAt == null
                ? order.createdAt.plus(properties.getPendingTimeout())
                : order.expiresAt;
        return !expiresAt.isAfter(now);
    }

    private void recordQueryResult(PaymentOrder order, PaymentQueryResult result, LocalDateTime now) {
        paymentOrderMapper.update(null, new UpdateWrapper<PaymentOrder>()
                .eq("id", order.id)
                .eq("pay_status", "CREATED")
                .set("provider_status", result.providerStatus())
                .set(result.providerTradeNo() != null && !result.providerTradeNo().isBlank(),
                        "provider_trade_no", result.providerTradeNo())
                .set("last_queried_at", now)
                .setSql("query_attempt_count = query_attempt_count + 1")
                .set("last_query_error", null));
    }

    private void scheduleNextQuery(PaymentOrder order, LocalDateTime now, String error) {
        paymentOrderMapper.update(null, new UpdateWrapper<PaymentOrder>()
                .eq("id", order.id)
                .eq("pay_status", "CREATED")
                .set("next_query_at", now.plus(properties.getRetryDelay()))
                .set("last_query_error", error));
    }

    private boolean markClosed(PaymentOrder order, LocalDateTime now, String reason, String providerStatus) {
        int updated = paymentOrderMapper.update(null, new UpdateWrapper<PaymentOrder>()
                .eq("id", order.id)
                .eq("pay_status", "CREATED")
                .set("pay_status", "CLOSED")
                .set("provider_status", providerStatus)
                .set("closed_at", now)
                .set("close_reason", reason)
                .set("next_query_at", null)
                .set("last_query_error", null)
                .set("idempotency_active", null));
        if (updated > 0) {
            operationLogService.record(OperationModule.PAYMENT, "AUTO_CLOSE_ORDER", "payment_order", order.id,
                    "close unpaid payment order", Map.of("orderNo", order.orderNo, "reason", reason));
        }
        return updated > 0;
    }

    private void recordFailure(PaymentOrder order, LocalDateTime now, RuntimeException ex) {
        String message = truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 1000);
        paymentOrderMapper.update(null, new UpdateWrapper<PaymentOrder>()
                .eq("id", order.id)
                .eq("pay_status", "CREATED")
                .set("last_queried_at", now)
                .setSql("query_attempt_count = query_attempt_count + 1")
                .set("next_query_at", now.plus(properties.getRetryDelay()))
                .set("last_query_error", message));
        operationLogService.record(OperationModule.PAYMENT, "PAYMENT_QUERY_FAILED", "payment_order", order.id,
                "payment query or close failed", Map.of("orderNo", order.orderNo, "error", message));
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private enum ReconcileOutcome {
        PAID,
        CLOSED,
        PENDING,
        FAILED
    }
}
