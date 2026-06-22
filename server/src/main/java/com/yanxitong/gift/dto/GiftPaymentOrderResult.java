package com.yanxitong.gift.dto;

import com.yanxitong.payment.PaymentOrderCreateResult;
import com.yanxitong.payment.entity.PaymentOrder;

public record GiftPaymentOrderResult(PaymentOrder order, String payPayload) {
    public static GiftPaymentOrderResult from(PaymentOrderCreateResult result) {
        return new GiftPaymentOrderResult(result.order(), result.payPayload());
    }
}
