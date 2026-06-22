package com.yanxitong.security;

import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicRateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final OperationLogService operationLogService;

    public PublicRateLimitService(StringRedisTemplate redisTemplate, OperationLogService operationLogService) {
        this.redisTemplate = redisTemplate;
        this.operationLogService = operationLogService;
    }

    public void check(HttpServletRequest request, String scope, int limit, Duration window, String... dimensions) {
        String ip = clientIp(request);
        String key = "rate:public:" + scope + ":" + hash(ip + "|" + String.join("|", dimensions));
        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, window);
            }
        } catch (RedisSystemException ex) {
            return;
        } catch (RuntimeException ex) {
            return;
        }
        if (count != null && count > limit) {
            operationLogService.record(
                    OperationModule.SECURITY,
                    "PUBLIC_RATE_LIMIT",
                    "public_endpoint",
                    null,
                    "public endpoint rate limit exceeded",
                    Map.of(
                            "scope", scope,
                            "ip", ip,
                            "limit", limit,
                            "windowSeconds", window.toSeconds(),
                            "dimensions", dimensions
                    )
            );
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
