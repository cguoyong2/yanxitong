package com.yanxitong.common;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final SecurityReadinessService securityReadinessService;

    public HealthController(SecurityReadinessService securityReadinessService) {
        this.securityReadinessService = securityReadinessService;
    }

    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "time", OffsetDateTime.now().toString()));
    }

    @GetMapping("/api/health/readiness")
    public ApiResponse<SecurityReadinessResult> readiness() {
        return ApiResponse.ok(securityReadinessService.check());
    }
}
