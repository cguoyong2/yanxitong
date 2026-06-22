package com.yanxitong.payment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentAdapterRegistry {
    private final Map<PaymentProvider, PaymentAdapter> adapters = new EnumMap<>(PaymentProvider.class);

    public PaymentAdapterRegistry(List<PaymentAdapter> adapters) {
        adapters.forEach(adapter -> this.adapters.put(adapter.provider(), adapter));
    }

    public PaymentAdapter get(PaymentProvider provider) {
        PaymentAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        return adapter;
    }
}

