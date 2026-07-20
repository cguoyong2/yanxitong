package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WechatDirectAdapterTests {
    @Test
    void createPaymentBuildsDirectJsapiPrepayRequest() {
        CapturingDirectJsapiClient client = new CapturingDirectJsapiClient(new WechatPrepayResult(
                "wx-prepay-id",
                "{\"package\":\"prepay_id=wx-prepay-id\"}"
        ));
        WechatDirectAdapter adapter = newAdapter(completeProperties(), client);

        PaymentCreateResult result = adapter.createPayment(new PaymentCreateCommand(
                "GP202606220001",
                PaymentScene.ONLINE_GIFT,
                new BigDecimal("12.34"),
                "婚礼随礼",
                1L,
                10L,
                "payer-openid"
        ));

        assertEquals("wx-prepay-id", result.prepayId());
        assertEquals("{\"package\":\"prepay_id=wx-prepay-id\"}", result.payPayload());
        assertNull(result.providerTradeNo());
        assertEquals("wx-direct-app", client.request.getAppid());
        assertEquals("direct-merchant", client.request.getMchid());
        assertEquals("GP202606220001", client.request.getOutTradeNo());
        assertEquals("婚礼随礼", client.request.getDescription());
        assertEquals("https://example.com/api/payments/callbacks/wechat-direct", client.request.getNotifyUrl());
        assertEquals(1234, client.request.getAmount().getTotal());
        assertEquals("CNY", client.request.getAmount().getCurrency());
        assertEquals("payer-openid", client.request.getPayer().getOpenid());
    }

    @Test
    void createPaymentRejectsMissingPayerOpenid() {
        WechatDirectAdapter adapter = newAdapter(completeProperties(), new CapturingDirectJsapiClient());

        assertThrows(IllegalArgumentException.class, () -> adapter.createPayment(command("", new BigDecimal("1.00"))));
    }

    @Test
    void createPaymentRejectsDisabledProvider() {
        PaymentProviderProperties properties = completeProperties();
        properties.provider(PaymentProvider.WECHAT_DIRECT).setEnabled(false);
        WechatDirectAdapter adapter = newAdapter(properties, new CapturingDirectJsapiClient());

        assertThrows(UnsupportedOperationException.class, () -> adapter.createPayment(command("openid", new BigDecimal("1.00"))));
    }

    @Test
    void verifyAndParseCallbackUsesVerifiedWechatTransaction() {
        FakeNotificationParserClient parserClient = new FakeNotificationParserClient(new WechatCallbackParseResult(
                transaction("GP202606220003", "4200000000000001", 1234, Transaction.TradeStateEnum.SUCCESS),
                "{\"out_trade_no\":\"GP202606220003\"}",
                "evt-1",
                "TRANSACTION.SUCCESS",
                "encrypt-resource",
                "SERIAL"
        ));
        WechatDirectAdapter adapter = newAdapter(completeProperties(), new CapturingDirectJsapiClient(), parserClient);

        PaymentCallbackResult result = adapter.verifyAndParseCallback(new PaymentCallbackEnvelope(
                PaymentProvider.WECHAT_DIRECT,
                "{\"id\":\"evt-1\"}",
                Map.of("Wechatpay-Serial", "SERIAL"),
                "signature",
                "request-id"
        ));

        assertEquals("GP202606220003", result.orderNo());
        assertEquals("4200000000000001", result.providerTradeNo());
        assertEquals(0, new BigDecimal("12.34").compareTo(result.paidAmount()));
        assertEquals("evt-1", result.providerEventId());
        assertEquals("SERIAL", result.providerSerialNo());
        assertEquals("TRANSACTION.SUCCESS", result.eventType());
        assertEquals("encrypt-resource", result.resourceType());
        assertEquals("{\"out_trade_no\":\"GP202606220003\"}", result.decryptedBody());
    }

    @Test
    void queryAndCloseUseDirectMerchantOrderApi() {
        CapturingDirectJsapiClient client = new CapturingDirectJsapiClient();
        client.queryResult = transaction("GP202606220005", "WX005", 1234, Transaction.TradeStateEnum.SUCCESS);
        WechatDirectAdapter adapter = newAdapter(completeProperties(), client);

        PaymentQueryResult result = adapter.queryPayment("GP202606220005");
        adapter.closePayment("GP202606220006");

        assertEquals("SUCCESS", result.providerStatus());
        assertEquals("WX005", result.providerTradeNo());
        assertEquals(0, new BigDecimal("12.34").compareTo(result.paidAmount()));
        assertEquals("GP202606220005", client.queriedOrderNo);
        assertEquals("direct-merchant", client.queriedMerchantId);
        assertEquals("GP202606220006", client.closedOrderNo);
        assertEquals("direct-merchant", client.closedMerchantId);
    }

    private WechatDirectAdapter newAdapter(
            PaymentProviderProperties properties,
            WechatDirectJsapiClient client
    ) {
        return newAdapter(properties, client, envelope -> {
            throw new AssertionError("notification parser should not be called");
        });
    }

    private WechatDirectAdapter newAdapter(
            PaymentProviderProperties properties,
            WechatDirectJsapiClient client,
            WechatNotificationParserClient parserClient
    ) {
        return new WechatDirectAdapter(properties, client, parserClient);
    }

    private PaymentCreateCommand command(String payerOpenId, BigDecimal amount) {
        return new PaymentCreateCommand(
                "GP202606220002",
                PaymentScene.ONLINE_GIFT,
                amount,
                "婚礼随礼",
                1L,
                10L,
                payerOpenId
        );
    }

    private PaymentProviderProperties completeProperties() {
        PaymentProviderProperties.ProviderConfig config = new PaymentProviderProperties.ProviderConfig();
        config.setEnabled(true);
        config.setMerchantId("direct-merchant");
        config.setAppId("wx-direct-app");
        config.setNotifyUrl("https://example.com/api/payments/callbacks/wechat-direct");
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.setProviders(Map.of(PaymentProvider.WECHAT_DIRECT, config));
        return properties;
    }

    private Transaction transaction(
            String orderNo,
            String transactionId,
            Integer cents,
            Transaction.TradeStateEnum tradeState
    ) {
        Transaction transaction = new Transaction();
        transaction.setOutTradeNo(orderNo);
        transaction.setTransactionId(transactionId);
        transaction.setTradeState(tradeState);
        TransactionAmount amount = new TransactionAmount();
        amount.setTotal(cents);
        amount.setCurrency("CNY");
        transaction.setAmount(amount);
        return transaction;
    }

    private static class CapturingDirectJsapiClient implements WechatDirectJsapiClient {
        private final WechatPrepayResult result;
        private PrepayRequest request;
        private Transaction queryResult;
        private String queriedOrderNo;
        private String queriedMerchantId;
        private String closedOrderNo;
        private String closedMerchantId;

        private CapturingDirectJsapiClient() {
            this(new WechatPrepayResult("prepay", "{}"));
        }

        private CapturingDirectJsapiClient(WechatPrepayResult result) {
            this.result = result;
        }

        @Override
        public WechatPrepayResult prepay(PrepayRequest request) {
            this.request = request;
            return result;
        }

        @Override
        public Transaction queryOrderByOutTradeNo(String orderNo, String merchantId) {
            this.queriedOrderNo = orderNo;
            this.queriedMerchantId = merchantId;
            return queryResult;
        }

        @Override
        public void closeOrder(String orderNo, String merchantId) {
            this.closedOrderNo = orderNo;
            this.closedMerchantId = merchantId;
        }
    }

    private static class FakeNotificationParserClient implements WechatNotificationParserClient {
        private final WechatCallbackParseResult result;

        private FakeNotificationParserClient(WechatCallbackParseResult result) {
            this.result = result;
        }

        @Override
        public WechatCallbackParseResult parse(PaymentCallbackEnvelope envelope) {
            return result;
        }
    }
}
