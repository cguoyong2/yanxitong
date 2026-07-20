package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WechatServiceProviderAdapterTests {
    @Test
    void createPaymentBuildsPartnerJsapiPrepayRequest() {
        CapturingJsapiClient client = new CapturingJsapiClient(new WechatPrepayResult(
                "wx-prepay-id",
                "{\"package\":\"prepay_id=wx-prepay-id\"}"
        ));
        WechatServiceProviderAdapter adapter = newAdapter(completeProperties(), client);

        PaymentCreateResult result = adapter.createPayment(new PaymentCreateCommand(
                "GP202606220001",
                PaymentScene.ONLINE_GIFT,
                new BigDecimal("12.34"),
                "婚礼随礼",
                1L,
                10L,
                "sub-openid"
        ));

        assertEquals("wx-prepay-id", result.prepayId());
        assertEquals("{\"package\":\"prepay_id=wx-prepay-id\"}", result.payPayload());
        assertNull(result.providerTradeNo());
        assertEquals("sub-merchant", client.subMerchantId);
        assertEquals("wx-sp-app", client.request.getSpAppid());
        assertEquals("sp-merchant", client.request.getSpMchid());
        assertEquals("wx-sub-app", client.request.getSubAppid());
        assertEquals("sub-merchant", client.request.getSubMchid());
        assertEquals("GP202606220001", client.request.getOutTradeNo());
        assertEquals("婚礼随礼", client.request.getDescription());
        assertEquals("https://example.com/api/payments/callbacks/wechat-service-provider", client.request.getNotifyUrl());
        assertEquals(1234, client.request.getAmount().getTotal());
        assertEquals("CNY", client.request.getAmount().getCurrency());
        assertEquals("sub-openid", client.request.getPayer().getSubOpenid());
        assertNull(client.request.getPayer().getSpOpenid());
    }

    @Test
    void createPaymentUsesServiceProviderOpenidWhenSubAppIdIsAbsent() {
        PaymentProviderProperties properties = completeProperties();
        properties.provider(PaymentProvider.WECHAT_SERVICE_PROVIDER).setSubAppId(null);
        CapturingJsapiClient client = new CapturingJsapiClient(new WechatPrepayResult("prepay", "{}"));
        WechatServiceProviderAdapter adapter = newAdapter(properties, client);

        adapter.createPayment(command("sp-openid", new BigDecimal("1.00")));

        assertNull(client.request.getSubAppid());
        assertEquals("sp-openid", client.request.getPayer().getSpOpenid());
        assertNull(client.request.getPayer().getSubOpenid());
    }

    @Test
    void createPaymentRejectsInvalidCentPrecision() {
        WechatServiceProviderAdapter adapter = newAdapter(completeProperties(), new CapturingJsapiClient());

        assertThrows(ArithmeticException.class, () -> adapter.createPayment(command("openid", new BigDecimal("1.001"))));
    }

    @Test
    void createPaymentRejectsMissingPayerOpenid() {
        WechatServiceProviderAdapter adapter = newAdapter(completeProperties(), new CapturingJsapiClient());

        assertThrows(IllegalArgumentException.class, () -> adapter.createPayment(command("", new BigDecimal("1.00"))));
    }

    @Test
    void createPaymentRejectsDisabledProvider() {
        PaymentProviderProperties properties = completeProperties();
        properties.provider(PaymentProvider.WECHAT_SERVICE_PROVIDER).setEnabled(false);
        WechatServiceProviderAdapter adapter = newAdapter(properties, new CapturingJsapiClient());

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
        WechatServiceProviderAdapter adapter = newAdapter(completeProperties(), new CapturingJsapiClient(), parserClient);

        PaymentCallbackResult result = adapter.verifyAndParseCallback(new PaymentCallbackEnvelope(
                PaymentProvider.WECHAT_SERVICE_PROVIDER,
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
    void verifyAndParseCallbackMarksNonSuccessTradeAsIgnoredResult() {
        FakeNotificationParserClient parserClient = new FakeNotificationParserClient(new WechatCallbackParseResult(
                transaction("GP202606220004", "4200000000000002", 100, Transaction.TradeStateEnum.NOTPAY),
                "{}",
                "evt-2",
                "TRANSACTION.NOTPAY",
                "encrypt-resource",
                "SERIAL"
        ));
        WechatServiceProviderAdapter adapter = newAdapter(completeProperties(), new CapturingJsapiClient(), parserClient);

        PaymentCallbackResult result = adapter.verifyAndParseCallback(new PaymentCallbackEnvelope(
                PaymentProvider.WECHAT_SERVICE_PROVIDER,
                "{}",
                Map.of(),
                "signature",
                null
        ));

        assertEquals("GP202606220004", result.orderNo());
        assertEquals(0, new BigDecimal("1.00").compareTo(result.paidAmount()));
        assertEquals(false, result.success());
    }

    @Test
    void queryAndCloseUsePartnerMerchantOrderApi() {
        CapturingJsapiClient client = new CapturingJsapiClient();
        com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction transaction =
                new com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction();
        transaction.setOutTradeNo("GP202606220005");
        transaction.setTransactionId("WX005");
        transaction.setTradeState(com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction.TradeStateEnum.SUCCESS);
        com.wechat.pay.java.service.partnerpayments.model.TransactionAmount amount =
                new com.wechat.pay.java.service.partnerpayments.model.TransactionAmount();
        amount.setTotal(1234);
        amount.setCurrency("CNY");
        transaction.setAmount(amount);
        client.queryResult = transaction;
        WechatServiceProviderAdapter adapter = newAdapter(completeProperties(), client);

        PaymentQueryResult result = adapter.queryPayment("GP202606220005");
        adapter.closePayment("GP202606220006");

        assertEquals("SUCCESS", result.providerStatus());
        assertEquals("WX005", result.providerTradeNo());
        assertEquals(0, new BigDecimal("12.34").compareTo(result.paidAmount()));
        assertEquals("GP202606220005", client.queriedOrderNo);
        assertEquals("sp-merchant", client.queriedServiceProviderId);
        assertEquals("sub-merchant", client.queriedSubMerchantId);
        assertEquals("GP202606220006", client.closedOrderNo);
    }

    private WechatServiceProviderAdapter newAdapter(
            PaymentProviderProperties properties,
            WechatPartnerJsapiClient client
    ) {
        return newAdapter(properties, client, envelope -> {
            throw new AssertionError("notification parser should not be called");
        });
    }

    private WechatServiceProviderAdapter newAdapter(
            PaymentProviderProperties properties,
            WechatPartnerJsapiClient client,
            WechatNotificationParserClient parserClient
    ) {
        return new WechatServiceProviderAdapter(properties, client, parserClient);
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
        config.setAppId("wx-sp-app");
        config.setServiceProviderId("sp-merchant");
        config.setSubMerchantId("sub-merchant");
        config.setSubAppId("wx-sub-app");
        config.setNotifyUrl("https://example.com/api/payments/callbacks/wechat-service-provider");
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.setProviders(Map.of(PaymentProvider.WECHAT_SERVICE_PROVIDER, config));
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

    private static class CapturingJsapiClient implements WechatPartnerJsapiClient {
        private final WechatPrepayResult result;
        private PrepayRequest request;
        private String subMerchantId;
        private com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction queryResult;
        private String queriedOrderNo;
        private String queriedServiceProviderId;
        private String queriedSubMerchantId;
        private String closedOrderNo;

        private CapturingJsapiClient() {
            this(new WechatPrepayResult("prepay", "{}"));
        }

        private CapturingJsapiClient(WechatPrepayResult result) {
            this.result = result;
        }

        @Override
        public WechatPrepayResult prepay(PrepayRequest request, String subMerchantId) {
            this.request = request;
            this.subMerchantId = subMerchantId;
            return result;
        }

        @Override
        public com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction queryOrderByOutTradeNo(
                String orderNo,
                String serviceProviderId,
                String subMerchantId
        ) {
            this.queriedOrderNo = orderNo;
            this.queriedServiceProviderId = serviceProviderId;
            this.queriedSubMerchantId = subMerchantId;
            return queryResult;
        }

        @Override
        public void closeOrder(String orderNo, String serviceProviderId, String subMerchantId) {
            this.closedOrderNo = orderNo;
            this.queriedServiceProviderId = serviceProviderId;
            this.queriedSubMerchantId = subMerchantId;
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
