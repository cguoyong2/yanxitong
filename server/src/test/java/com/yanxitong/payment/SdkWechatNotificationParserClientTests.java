package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.service.payments.model.Transaction;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SdkWechatNotificationParserClientTests {
    @Test
    void parsesDecryptedResourceAsTransactionAndKeepsOuterEventMetadata() {
        WechatPayClientFactory factory = mock(WechatPayClientFactory.class);
        NotificationParser parser = mock(NotificationParser.class);
        when(factory.prepare(PaymentProvider.WECHAT_DIRECT))
                .thenReturn(new WechatPayClientFactory.PreparedWechatPayClient(null, parser, "AUTO"));

        Transaction transaction = new Transaction();
        transaction.setOutTradeNo("GP202607260001");
        transaction.setTransactionId("WX202607260001");
        transaction.setTradeState(Transaction.TradeStateEnum.SUCCESS);
        when(parser.parse(any(), eq(Transaction.class))).thenReturn(transaction);

        SdkWechatNotificationParserClient client =
                new SdkWechatNotificationParserClient(factory, new ObjectMapper());
        PaymentCallbackEnvelope envelope = new PaymentCallbackEnvelope(
                PaymentProvider.WECHAT_DIRECT,
                """
                        {
                          "id": "EVT-1",
                          "event_type": "TRANSACTION.SUCCESS",
                          "resource_type": "encrypt-resource",
                          "resource": {
                            "algorithm": "AEAD_AES_256_GCM",
                            "ciphertext": "redacted",
                            "associated_data": "transaction",
                            "nonce": "redacted"
                          }
                        }
                        """,
                Map.of(
                        "Wechatpay-Serial", "SERIAL",
                        "Wechatpay-Timestamp", "1753488000",
                        "Wechatpay-Nonce", "NONCE",
                        "Wechatpay-Signature", "SIGNATURE"
                ),
                "SIGNATURE",
                "REQUEST-1"
        );

        WechatCallbackParseResult result = client.parse(envelope);

        verify(parser).parse(any(), eq(Transaction.class));
        assertEquals("GP202607260001", result.transaction().getOutTradeNo());
        assertEquals("EVT-1", result.providerEventId());
        assertEquals("TRANSACTION.SUCCESS", result.eventType());
        assertEquals("encrypt-resource", result.resourceType());
        assertNotNull(result.decryptedBody());
    }
}
