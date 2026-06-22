package com.yanxitong.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentAdapter implements PaymentAdapter {
    private final ObjectMapper objectMapper;
    private final PaymentProviderProperties properties;

    public MockPaymentAdapter(ObjectMapper objectMapper, PaymentProviderProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MOCK;
    }

    @Override
    public PaymentCreateResult createPayment(PaymentCreateCommand command) {
        return new PaymentCreateResult(command.orderNo(), "MOCK-" + command.orderNo(), "{}");
    }

    @Override
    public PaymentCallbackResult verifyAndParseCallback(PaymentCallbackEnvelope envelope) {
        try {
            PaymentProviderProperties.ProviderConfig config = properties.provider(PaymentProvider.MOCK);
            if (config.hasCallbackSecret()) {
                String expected = PaymentSignature.hmacSha256Hex(envelope.rawBody(), config.getCallbackSecret());
                if (!PaymentSignature.matches(expected, envelope.signature())) {
                    throw new IllegalArgumentException("mock callback signature verification failed");
                }
            }
            JsonNode root = objectMapper.readTree(envelope.rawBody());
            String orderNo = root.path("orderNo").asText("");
            if (orderNo.isBlank()) {
                throw new IllegalArgumentException("mock callback orderNo is required");
            }
            String providerTradeNo = root.path("providerTradeNo").asText("MOCK-" + orderNo);
            BigDecimal paidAmount = root.hasNonNull("paidAmount")
                    ? root.path("paidAmount").decimalValue()
                    : null;
            boolean success = !root.has("success") || root.path("success").asBoolean();
            return new PaymentCallbackResult(orderNo, providerTradeNo, paidAmount, success);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid mock callback payload: " + ex.getMessage(), ex);
        }
    }
}
