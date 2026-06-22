package com.yanxitong.payment;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MockPaymentGuard {
    private final PaymentProviderProperties properties;

    public MockPaymentGuard(PaymentProviderProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isMockSuccessEnabled();
    }

    public void requireEnabled() {
        if (!isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "mock payment success is disabled");
        }
    }
}
