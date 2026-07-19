package com.yanxitong.miniapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MiniappTokenService {
    private static final String TOKEN_PREFIX = "auth:miniapp:";
    public static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MiniappTokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String issue(MiniappPrincipal principal) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(TOKEN_PREFIX + token, objectMapper.writeValueAsString(principal), TOKEN_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize miniapp token", ex);
        }
        return token;
    }

    public Optional<MiniappPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String raw = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            redisTemplate.expire(TOKEN_PREFIX + token, TOKEN_TTL);
            return Optional.of(objectMapper.readValue(raw, MiniappPrincipal.class));
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        }
    }
}
