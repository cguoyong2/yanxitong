package com.yanxitong.miniapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class MiniappTokenServiceTests {
    @Test
    void issueAndResolveUseRedisBackedSlidingSession() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        ObjectMapper objectMapper = new ObjectMapper();
        MiniappPrincipal principal = new MiniappPrincipal(12L, 3L, "openid-12", "USER");
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(objectMapper.writeValueAsString(principal));
        MiniappTokenService service = new MiniappTokenService(redisTemplate, objectMapper);

        String token = service.issue(principal);
        Optional<MiniappPrincipal> resolved = service.resolve(token);

        assertEquals(32, token.length());
        assertEquals(principal, resolved.orElseThrow());
        verify(values).set("auth:miniapp:" + token, objectMapper.writeValueAsString(principal), MiniappTokenService.TOKEN_TTL);
        verify(redisTemplate).expire("auth:miniapp:" + token, MiniappTokenService.TOKEN_TTL);
    }

    @Test
    void resolveRejectsBlankAndMalformedSessions() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(startsWith("auth:miniapp:"))).thenReturn("not-json");
        MiniappTokenService service = new MiniappTokenService(redisTemplate, new ObjectMapper());

        assertTrue(service.resolve("").isEmpty());
        assertTrue(service.resolve("bad-token").isEmpty());
    }
}
