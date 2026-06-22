package com.yanxitong;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yanxitong.payment.PaymentProvider;
import org.junit.jupiter.api.Test;

class YanxitongApplicationTests {
    @Test
    void paymentProviderEnumIsAvailable() {
        assertNotNull(PaymentProvider.WECHAT_SERVICE_PROVIDER);
    }
}
