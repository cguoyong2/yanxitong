package com.yanxitong.payment;

public interface WechatNotificationParserClient {
    WechatCallbackParseResult parse(PaymentCallbackEnvelope envelope);
}
