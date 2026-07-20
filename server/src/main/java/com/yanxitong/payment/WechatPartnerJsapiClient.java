package com.yanxitong.payment;

import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction;

public interface WechatPartnerJsapiClient {
    WechatPrepayResult prepay(PrepayRequest request, String subMerchantId);

    Transaction queryOrderByOutTradeNo(String orderNo, String serviceProviderId, String subMerchantId);

    void closeOrder(String orderNo, String serviceProviderId, String subMerchantId);
}
