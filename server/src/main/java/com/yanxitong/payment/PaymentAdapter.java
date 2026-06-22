package com.yanxitong.payment;

public interface PaymentAdapter {
    PaymentProvider provider();

    PaymentCreateResult createPayment(PaymentCreateCommand command);

    PaymentCallbackResult verifyAndParseCallback(PaymentCallbackEnvelope envelope);
}
