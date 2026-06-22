package com.yanxitong.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class PublicRateLimitServiceTests {
    @Test
    void allowsRequestsUnderLimit() {
        OperationLogService operationLogService = mock(OperationLogService.class);
        PublicRateLimitService service = new PublicRateLimitService(redisTemplateReturning(2L), operationLogService);

        assertDoesNotThrow(() -> service.check(request(), "rsvp-submit", 3, Duration.ofMinutes(1), "1", "guest"));

        verify(operationLogService, never()).record(eq(OperationModule.SECURITY), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsAndLogsRequestsOverLimit() {
        OperationLogService operationLogService = mock(OperationLogService.class);
        PublicRateLimitService service = new PublicRateLimitService(redisTemplateReturning(4L), operationLogService);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.check(request(), "gift-payment-order-create", 3, Duration.ofMinutes(1), "1", "guest"));

        assertEquals(429, ex.getStatusCode().value());
        verify(operationLogService).record(eq(OperationModule.SECURITY), eq("PUBLIC_RATE_LIMIT"), eq("public_endpoint"), eq(null), any(), any());
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        OperationLogService operationLogService = mock(OperationLogService.class);
        PublicRateLimitService service = new PublicRateLimitService(redisTemplate, operationLogService);

        assertDoesNotThrow(() -> service.check(request(), "invitation-public-view", 1, Duration.ofMinutes(1), "slug"));

        verify(operationLogService, never()).record(eq(OperationModule.SECURITY), any(), any(), any(), any(), any());
    }

    private StringRedisTemplate redisTemplateReturning(Long count) {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any(String.class))).thenReturn(count);
        return redisTemplate;
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        return request;
    }
}
