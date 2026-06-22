package com.yanxitong.payment;

import com.yanxitong.payment.entity.PaymentOrder;

public record PaymentOrderCreateResult(PaymentOrder order, String payPayload) {
}
