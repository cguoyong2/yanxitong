package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockPaymentAdapterTests {
    private static final String SECRET = "test-secret";

    @Test
    void verifyAndParseCallbackAcceptsSignedEnvelope() {
        MockPaymentAdapter adapter = new MockPaymentAdapter(new ObjectMapper(), properties());
        String rawBody = "{\"orderNo\":\"GP_TEST\",\"providerTradeNo\":\"MOCK-GP_TEST\",\"paidAmount\":12.34,\"success\":true}";
        String signature = PaymentSignature.hmacSha256Hex(rawBody, SECRET);

        PaymentCallbackResult result = adapter.verifyAndParseCallback(new PaymentCallbackEnvelope(
                PaymentProvider.MOCK,
                rawBody,
                Map.of("X-Test", "1"),
                signature,
                "req-1"
        ));

        assertEquals("GP_TEST", result.orderNo());
        assertEquals("MOCK-GP_TEST", result.providerTradeNo());
        assertEquals(0, new BigDecimal("12.34").compareTo(result.paidAmount()));
        assertTrue(result.success());
    }

    @Test
    void verifyAndParseCallbackRejectsInvalidSignature() {
        MockPaymentAdapter adapter = new MockPaymentAdapter(new ObjectMapper(), properties());
        String rawBody = "{\"orderNo\":\"GP_TEST\",\"paidAmount\":12.34}";

        assertThrows(IllegalArgumentException.class, () -> adapter.verifyAndParseCallback(new PaymentCallbackEnvelope(
                PaymentProvider.MOCK,
                rawBody,
                Map.of(),
                "bad-signature",
                null
        )));
    }

    private PaymentProviderProperties properties() {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        PaymentProviderProperties.ProviderConfig config = new PaymentProviderProperties.ProviderConfig();
        config.setEnabled(true);
        config.setCallbackSecret(SECRET);
        properties.setProviders(Map.of(PaymentProvider.MOCK, config));
        return properties;
    }
}
