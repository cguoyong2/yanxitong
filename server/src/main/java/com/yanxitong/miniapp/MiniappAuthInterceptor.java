package com.yanxitong.miniapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MiniappAuthInterceptor implements HandlerInterceptor {
    private final MiniappTokenService tokenService;
    private final ObjectMapper objectMapper;

    public MiniappAuthInterceptor(MiniappTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        boolean authenticationRequired = requiresAuthentication(method);
        Optional<MiniappPrincipal> principal = tokenService.resolve(bearerToken(request));
        if (principal.isEmpty()) {
            if (!authenticationRequired) {
                return true;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(401, "请先登录或重新进入小程序"));
            return false;
        }
        MiniappPrincipalContext.set(principal.get());
        TenantContext.setTenantId(principal.get().tenantId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MiniappPrincipalContext.clear();
    }

    private boolean requiresAuthentication(HandlerMethod method) {
        return method.hasMethodAnnotation(MiniappAuthenticated.class)
                || method.getBeanType().isAnnotationPresent(MiniappAuthenticated.class);
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
