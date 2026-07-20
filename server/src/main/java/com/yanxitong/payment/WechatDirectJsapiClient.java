package com.yanxitong.payment;

import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.model.Transaction;

public interface WechatDirectJsapiClient {
    WechatPrepayResult prepay(PrepayRequest request);

    Transaction queryOrderByOutTradeNo(String orderNo, String merchantId);

    void closeOrder(String orderNo, String merchantId);
}
