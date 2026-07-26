package com.yanxitong.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class SdkWechatNotificationParserClient implements WechatNotificationParserClient {
    private final WechatPayClientFactory clientFactory;
    private final ObjectMapper objectMapper;

    public SdkWechatNotificationParserClient(WechatPayClientFactory clientFactory, ObjectMapper objectMapper) {
        this.clientFactory = clientFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public WechatCallbackParseResult parse(PaymentCallbackEnvelope envelope) {
        NotificationParser parser = clientFactory.prepare(envelope.provider()).notificationParser();
        if (parser == null) {
            throw new IllegalStateException("Wechat notification parser is not configured");
        }
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(requiredHeader(envelope, "Wechatpay-Serial"))
                .timestamp(requiredHeader(envelope, "Wechatpay-Timestamp"))
                .nonce(requiredHeader(envelope, "Wechatpay-Nonce"))
                .signature(requiredHeader(envelope, "Wechatpay-Signature"))
                .body(envelope.rawBody())
                .build();
        try {
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            if (transaction == null) {
                throw new IllegalArgumentException("Wechat callback decrypted transaction is empty");
            }
            JsonNode notification = objectMapper.readTree(envelope.rawBody());
            String decryptedBody = objectMapper.writeValueAsString(transaction);
            return new WechatCallbackParseResult(
                    transaction,
                    decryptedBody,
                    text(notification, "id"),
                    text(notification, "event_type"),
                    text(notification, "resource_type"),
                    envelope.header("Wechatpay-Serial")
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid Wechat callback payload: " + ex.getMessage(), ex);
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String requiredHeader(PaymentCallbackEnvelope envelope, String name) {
        String value = envelope.header(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Wechat callback header is required: " + name);
        }
        return value;
    }
}
