package com.yanxitong.payment;

import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.order.OrderNoGenerator;
import com.yanxitong.payment.entity.PaymentOrder;
import com.yanxitong.payment.mapper.PaymentOrderMapper;
import com.yanxitong.tenant.TenantContext;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentAdapterRegistry paymentAdapterRegistry;
    private final OrderNoGenerator orderNoGenerator;
    private final OperationLogService operationLogService;
    private final PaymentProviderProperties properties;
    private final PaymentProviderReadinessService readinessService;

    public PaymentService(
            PaymentOrderMapper paymentOrderMapper,
            PaymentAdapterRegistry paymentAdapterRegistry,
            OrderNoGenerator orderNoGenerator,
            OperationLogService operationLogService,
            PaymentProviderProperties properties,
            PaymentProviderReadinessService readinessService
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentAdapterRegistry = paymentAdapterRegistry;
        this.orderNoGenerator = orderNoGenerator;
        this.operationLogService = operationLogService;
        this.properties = properties;
        this.readinessService = readinessService;
    }

    public PaymentOrderCreateResult createOrder(PaymentOrderCreateCommand command) {
        PaymentOrder existing = findExistingOrder(command.clientRequestId());
        if (existing != null) {
            return new PaymentOrderCreateResult(existing, existing.payPayload);
        }

        PaymentProvider defaultProvider = properties.getDefaultProvider();
        readinessService.requireCreateReady(defaultProvider);
        PaymentOrder order = new PaymentOrder();
        order.tenantId = TenantContext.getTenantId();
        order.banquetId = command.banquetId();
        order.orderNo = orderNoGenerator.next("GP");
        order.clientRequestId = normalize(command.clientRequestId());
        order.provider = defaultProvider.name();
        order.scene = command.scene().name();
        order.entrySource = command.entrySource();
        order.bizOrderType = normalize(command.bizOrderType());
        order.bizOrderNo = normalize(command.bizOrderNo());
        order.amount = command.amount();
        order.currency = "CNY";
        order.subject = command.subject();
        order.payerName = command.payerName();
        order.payerOpenId = command.payerOpenId();
        order.blessing = command.blessing();
        order.payStatus = "CREATED";

        PaymentAdapter adapter = paymentAdapterRegistry.get(defaultProvider);
        PaymentCreateResult createResult = adapter.createPayment(new PaymentCreateCommand(
                order.orderNo,
                command.scene(),
                order.amount,
                order.subject,
                order.tenantId,
                order.banquetId,
                order.payerOpenId
        ));
        order.providerTradeNo = createResult.providerTradeNo();
        order.prepayId = createResult.prepayId();
        order.payPayload = createResult.payPayload();
        order.expiresAt = createResult.expiresAt();
        paymentOrderMapper.insert(order);
        operationLogService.record(OperationModule.PAYMENT, "CREATE_ORDER", "payment_order", order.id, "create payment order");
        return new PaymentOrderCreateResult(order, createResult.payPayload());
    }

    private PaymentOrder findExistingOrder(String clientRequestId) {
        String normalized = normalize(clientRequestId);
        if (normalized == null) {
            return null;
        }
        QueryWrapper<PaymentOrder> query = new QueryWrapper<PaymentOrder>()
                .eq("client_request_id", normalized)
                .last("LIMIT 1");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.eq("tenant_id", tenantId);
        } else {
            query.isNull("tenant_id");
        }
        return paymentOrderMapper.selectOne(query);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
