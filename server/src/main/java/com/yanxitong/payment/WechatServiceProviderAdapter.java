package com.yanxitong.payment;

import com.wechat.pay.java.service.partnerpayments.jsapi.model.Amount;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Payer;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class WechatServiceProviderAdapter implements PaymentAdapter {
    private final PaymentProviderProperties properties;
    private final WechatPartnerJsapiClient jsapiClient;
    private final WechatNotificationParserClient notificationParserClient;

    public WechatServiceProviderAdapter(
            PaymentProviderProperties properties,
            WechatPartnerJsapiClient jsapiClient,
            WechatNotificationParserClient notificationParserClient
    ) {
        this.properties = properties;
        this.jsapiClient = jsapiClient;
        this.notificationParserClient = notificationParserClient;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.WECHAT_SERVICE_PROVIDER;
    }

    @Override
    public PaymentCreateResult createPayment(PaymentCreateCommand command) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider());
        validatePrepay(config, command);
        PrepayRequest request = buildPrepayRequest(config, command);
        WechatPrepayResult prepayResult = jsapiClient.prepay(request, config.getSubMerchantId());
        return new PaymentCreateResult(command.orderNo(), null, prepayResult.payPayload(), prepayResult.prepayId(), null);
    }

    @Override
    public PaymentCallbackResult verifyAndParseCallback(PaymentCallbackEnvelope envelope) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider());
        if (!config.isEnabled()) {
            throw new UnsupportedOperationException("Wechat service provider is not enabled");
        }
        WechatCallbackParseResult parsed = notificationParserClient.parse(envelope);
        Transaction transaction = parsed.transaction();
        String orderNo = transaction.getOutTradeNo();
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("Wechat callback out_trade_no is required");
        }
        boolean success = Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState());
        return new PaymentCallbackResult(
                orderNo,
                transaction.getTransactionId(),
                paidAmount(transaction),
                success,
                parsed.providerEventId(),
                parsed.providerSerialNo(),
                parsed.eventType(),
                parsed.resourceType(),
                parsed.decryptedBody()
        );
    }

    @Override
    public PaymentQueryResult queryPayment(String orderNo) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider());
        validateOrderOperation(config, orderNo);
        com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction transaction =
                jsapiClient.queryOrderByOutTradeNo(orderNo, config.getServiceProviderId(), config.getSubMerchantId());
        return new PaymentQueryResult(
                transaction.getOutTradeNo(),
                transaction.getTransactionId(),
                paidAmount(transaction),
                transaction.getTradeState() == null ? null : transaction.getTradeState().name()
        );
    }

    @Override
    public void closePayment(String orderNo) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider());
        validateOrderOperation(config, orderNo);
        jsapiClient.closeOrder(orderNo, config.getServiceProviderId(), config.getSubMerchantId());
    }

    private PrepayRequest buildPrepayRequest(PaymentProviderProperties.ProviderConfig config, PaymentCreateCommand command) {
        PrepayRequest request = new PrepayRequest();
        request.setSpAppid(config.getAppId());
        request.setSpMchid(config.getServiceProviderId());
        request.setSubAppid(blankToNull(config.getSubAppId()));
        request.setSubMchid(config.getSubMerchantId());
        request.setDescription(command.subject());
        request.setOutTradeNo(command.orderNo());
        request.setNotifyUrl(config.getNotifyUrl());
        request.setAmount(amount(command.amount()));
        request.setPayer(payer(config, command.payerOpenId()));
        return request;
    }

    private Amount amount(BigDecimal value) {
        BigDecimal cents = value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY);
        if (cents.signum() <= 0 || cents.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("Wechat payment amount must be greater than 0 and within integer cents range");
        }
        Amount amount = new Amount();
        amount.setTotal(cents.intValueExact());
        amount.setCurrency("CNY");
        return amount;
    }

    private Payer payer(PaymentProviderProperties.ProviderConfig config, String payerOpenId) {
        Payer payer = new Payer();
        if (config.getSubAppId() != null && !config.getSubAppId().isBlank()) {
            payer.setSubOpenid(payerOpenId);
        } else {
            payer.setSpOpenid(payerOpenId);
        }
        return payer;
    }

    private BigDecimal paidAmount(Transaction transaction) {
        if (transaction.getAmount() == null || transaction.getAmount().getTotal() == null) {
            return null;
        }
        return BigDecimal.valueOf(transaction.getAmount().getTotal(), 2);
    }

    private BigDecimal paidAmount(com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction transaction) {
        if (transaction.getAmount() == null || transaction.getAmount().getTotal() == null) {
            return null;
        }
        return BigDecimal.valueOf(transaction.getAmount().getTotal(), 2);
    }

    private void validatePrepay(PaymentProviderProperties.ProviderConfig config, PaymentCreateCommand command) {
        if (!config.isEnabled()) {
            throw new UnsupportedOperationException("Wechat service provider is not enabled");
        }
        require(config.getAppId(), "Wechat app-id is required");
        require(config.getServiceProviderId(), "Wechat service-provider-id is required");
        require(config.getSubMerchantId(), "Wechat sub-merchant-id is required");
        require(config.getNotifyUrl(), "Wechat notify-url is required");
        require(command.orderNo(), "Wechat order-no is required");
        require(command.subject(), "Wechat payment subject is required");
        require(command.payerOpenId(), "Wechat payer openid is required");
        if (command.amount() == null) {
            throw new IllegalArgumentException("Wechat payment amount is required");
        }
    }

    private void validateOrderOperation(PaymentProviderProperties.ProviderConfig config, String orderNo) {
        if (!config.isEnabled()) {
            throw new UnsupportedOperationException("Wechat service provider is not enabled");
        }
        require(config.getServiceProviderId(), "Wechat service-provider-id is required");
        require(config.getSubMerchantId(), "Wechat sub-merchant-id is required");
        require(orderNo, "Wechat order-no is required");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
