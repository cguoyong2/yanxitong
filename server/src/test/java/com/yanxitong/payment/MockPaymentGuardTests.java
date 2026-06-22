package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MockPaymentGuardTests {
    @Test
    void disabledByDefaultRejectsMockSuccess() {
        MockPaymentGuard guard = new MockPaymentGuard(new PaymentProviderProperties());

        assertThrows(ResponseStatusException.class, guard::requireEnabled);
    }

    @Test
    void enabledAllowsMockSuccess() {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.setMockSuccessEnabled(true);
        MockPaymentGuard guard = new MockPaymentGuard(properties);

        assertDoesNotThrow(guard::requireEnabled);
    }
}
