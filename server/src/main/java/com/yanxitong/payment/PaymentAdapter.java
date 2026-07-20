package com.yanxitong.payment;

public interface PaymentAdapter {
    PaymentProvider provider();

    PaymentCreateResult createPayment(PaymentCreateCommand command);

    PaymentCallbackResult verifyAndParseCallback(PaymentCallbackEnvelope envelope);

    default PaymentQueryResult queryPayment(String orderNo) {
        throw new UnsupportedOperationException("Payment query is not supported by " + provider());
    }

    default void closePayment(String orderNo) {
        throw new UnsupportedOperationException("Payment close is not supported by " + provider());
    }
}
