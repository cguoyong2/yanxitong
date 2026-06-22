package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class WechatCallbackFixtureTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void redactedSuccessFixtureIsValidJsonAndContainsExpectedShape() throws Exception {
        JsonNode root = fixture("success.redacted.json");

        assertEquals("/api/payments/callbacks/wechat-service-provider", root.path("request").path("path").asText());
        assertEquals("TRANSACTION.SUCCESS", root.path("request").path("rawBody").path("event_type").asText());
        assertEquals("SUCCESS", root.path("decryptedBodyShape").path("trade_state").asText());
        assertEquals("SUCCESS", root.path("expectedParserResult").path("processStatus").asText());
        assertFalse(root.toString().contains("apiclient_key"));
    }

    @Test
    void redactedFailedSignatureFixtureIsValidJsonAndContainsExpectedShape() throws Exception {
        JsonNode root = fixture("failed-signature.redacted.json");

        assertEquals("REDACTED_INVALID_SIGNATURE", root.path("request").path("headers").path("Wechatpay-Signature").asText());
        assertEquals("FAILED", root.path("expectedParserResult").path("verifyStatus").asText());
        assertEquals("SIGNATURE_OR_DECRYPTION_FAILURE", root.path("expectedParserResult").path("errorCategory").asText());
        assertFalse(root.toString().contains("api-v3-key"));
    }

    private JsonNode fixture(String filename) throws Exception {
        String path = "/payment/wechat-callback-fixtures/" + filename;
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertNotNull(inputStream, "missing fixture " + path);
            return objectMapper.readTree(inputStream);
        }
    }
}
