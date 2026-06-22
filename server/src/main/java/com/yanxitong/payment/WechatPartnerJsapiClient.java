package com.yanxitong.payment;

import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayRequest;

public interface WechatPartnerJsapiClient {
    WechatPrepayResult prepay(PrepayRequest request, String subMerchantId);
}
