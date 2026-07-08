package com.yanxitong.payment;

import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;

public interface WechatDirectJsapiClient {
    WechatPrepayResult prepay(PrepayRequest request);
}
