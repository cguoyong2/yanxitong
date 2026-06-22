package com.yanxitong.payment;

import java.util.Collections;
import java.util.Map;

public record PaymentCallbackEnvelope(
        PaymentProvider provider,
        String rawBody,
        Map<String, String> headers,
        String signature,
        String requestId
) {
    public PaymentCallbackEnvelope {
        headers = headers == null ? Collections.emptyMap() : Map.copyOf(headers);
    }

    public String header(String name) {
        if (name == null) {
            return "";
        }
        String direct = headers.get(name);
        if (direct != null) {
            return direct;
        }
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }
}
