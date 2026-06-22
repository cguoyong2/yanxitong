package com.yanxitong.payment;

public record WechatPrepayResult(
        String prepayId,
        String payPayload
) {
}
