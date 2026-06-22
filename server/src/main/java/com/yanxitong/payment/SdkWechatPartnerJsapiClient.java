package com.yanxitong.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.service.partnerpayments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayWithRequestPaymentResponse;
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
        WechatPayClientFactory.PreparedWechatPayClient prepared = clientFactory.prepare();
        JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                .config(prepared.config())
                .build();
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request, subMerchantId);
        String payPayload = toPayPayload(response);
        return new WechatPrepayResult(extractPrepayId(response.getPackageVal()), payPayload);
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
