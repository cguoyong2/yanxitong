package com.yanxitong.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.notification.Notification;
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
        NotificationParser parser = clientFactory.prepare().notificationParser();
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
        Notification notification = parser.parse(requestParam, Notification.class);
        String plaintext = notification.getPlaintext();
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Wechat callback decrypted body is empty");
        }
        try {
            Transaction transaction = objectMapper.readValue(plaintext, Transaction.class);
            return new WechatCallbackParseResult(
                    transaction,
                    plaintext,
                    notification.getId(),
                    notification.getEventType(),
                    notification.getResourceType(),
                    envelope.header("Wechatpay-Serial")
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid Wechat decrypted transaction payload: " + ex.getMessage(), ex);
        }
    }

    private String requiredHeader(PaymentCallbackEnvelope envelope, String name) {
        String value = envelope.header(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Wechat callback header is required: " + name);
        }
        return value;
    }
}
