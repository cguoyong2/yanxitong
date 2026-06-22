package com.yanxitong.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {
    private static final String TOKEN_PREFIX = "auth:admin:";
    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthTokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String issue(AdminPrincipal principal) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(TOKEN_PREFIX + token, objectMapper.writeValueAsString(principal), TOKEN_TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize admin token", e);
        }
        return token;
    }

    public Optional<AdminPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String raw = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            redisTemplate.expire(TOKEN_PREFIX + token, TOKEN_TTL);
            return Optional.of(objectMapper.readValue(raw, AdminPrincipal.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
