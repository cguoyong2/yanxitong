package com.yanxitong.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.service.partnerpayments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.CloseOrderRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SdkWechatPartnerJsapiClient implements WechatPartnerJsapiClient {
    private static final String PREPAY_PREFIX = "prepay_id=";

    private final WechatPayClientFactory clientFactory;
    private final ObjectMapper objectMapper;

    public SdkWechatPartnerJsapiClient(WechatPayClientFactory clientFactory, ObjectMapper objectMapper) {
        this.clientFactory = clientFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public WechatPrepayResult prepay(PrepayRequest request, String subMerchantId) {
        JsapiServiceExtension service = service();
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request, subMerchantId);
        String payPayload = toPayPayload(response);
        return new WechatPrepayResult(extractPrepayId(response.getPackageVal()), payPayload);
    }

    @Override
    public Transaction queryOrderByOutTradeNo(String orderNo, String serviceProviderId, String subMerchantId) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setOutTradeNo(orderNo);
        request.setSpMchid(serviceProviderId);
        request.setSubMchid(subMerchantId);
        return service().queryOrderByOutTradeNo(request);
    }

    @Override
    public void closeOrder(String orderNo, String serviceProviderId, String subMerchantId) {
        CloseOrderRequest request = new CloseOrderRequest();
        request.setOutTradeNo(orderNo);
        request.setSpMchid(serviceProviderId);
        request.setSubMchid(subMerchantId);
        service().closeOrder(request);
    }

    private JsapiServiceExtension service() {
        WechatPayClientFactory.PreparedWechatPayClient prepared = clientFactory.prepare(PaymentProvider.WECHAT_SERVICE_PROVIDER);
        return new JsapiServiceExtension.Builder()
                .config(prepared.config())
                .build();
    }

    private String toPayPayload(PrepayWithRequestPaymentResponse response) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("appId", response.getAppId());
        payload.put("timeStamp", response.getTimeStamp());
        payload.put("nonceStr", response.getNonceStr());
        payload.put("package", response.getPackageVal());
        payload.put("signType", response.getSignType());
        payload.put("paySign", response.getPaySign());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize Wechat pay payload", ex);
        }
    }

    private String extractPrepayId(String packageVal) {
        if (packageVal == null || packageVal.isBlank()) {
            throw new IllegalStateException("Wechat prepay response package is empty");
        }
        if (packageVal.startsWith(PREPAY_PREFIX)) {
            return packageVal.substring(PREPAY_PREFIX.length());
        }
        return packageVal;
    }
}
