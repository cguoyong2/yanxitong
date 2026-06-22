package com.yanxitong.payment;

import com.wechat.pay.java.service.payments.model.Transaction;

public record WechatCallbackParseResult(
        Transaction transaction,
        String decryptedBody,
        String providerEventId,
        String eventType,
        String resourceType,
        String providerSerialNo
) {
}
