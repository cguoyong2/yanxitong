package com.yanxitong.operationlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanxitong.auth.AdminPrincipal;
import com.yanxitong.auth.AdminPrincipalContext;
import com.yanxitong.operationlog.entity.OperationLog;
import com.yanxitong.operationlog.mapper.OperationLogMapper;
import com.yanxitong.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogService(OperationLogMapper operationLogMapper, ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    public void record(OperationModule module, String action, String targetType, Long targetId, String summary) {
        record(module, action, targetType, targetId, summary, null);
    }

    public void record(OperationModule module, String action, String targetType, Long targetId, String summary, Object detail) {
        OperationLog log = new OperationLog();
        AdminPrincipal principal = AdminPrincipalContext.get();
        log.tenantId = TenantContext.getTenantId();
        if (log.tenantId == null && principal != null) {
            log.tenantId = principal.tenantId();
        }
        if (principal != null) {
            log.operatorId = principal.adminUserId();
            log.operatorType = "ADMIN";
        } else {
            log.operatorType = "SYSTEM";
        }
        log.module = module.name();
        log.action = action;
        log.targetType = targetType;
        log.targetId = targetId;
        log.summary = summary;
        log.detail = serializeDetail(detail);
        RequestInfo requestInfo = currentRequestInfo();
        log.ipAddress = requestInfo.ipAddress();
        log.userAgent = requestInfo.userAgent();
        log.createdAt = LocalDateTime.now();
        operationLogMapper.insert(log);
    }

    private String serializeDetail(Object detail) {
        if (detail == null) {
            return null;
        }
        if (detail instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            try {
                return objectMapper.writeValueAsString(Map.of("value", String.valueOf(detail)));
            } catch (JsonProcessingException ignored) {
                return null;
            }
        }
    }

    private RequestInfo currentRequestInfo() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new RequestInfo(null, null);
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ipAddress = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",")[0].trim();
        return new RequestInfo(ipAddress, request.getHeader("User-Agent"));
    }

    private record RequestInfo(String ipAddress, String userAgent) {
    }
}
