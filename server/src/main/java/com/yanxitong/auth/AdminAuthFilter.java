package com.yanxitong.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AdminAuthFilter extends OncePerRequestFilter {
    private final AuthTokenService tokenService;
    private final ObjectMapper objectMapper;

    public AdminAuthFilter(AuthTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<AdminPrincipal> principal = tokenService.resolve(bearerToken(request));
        if (principal.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(401, "未登录或登录已过期"));
            return;
        }

        try {
            AdminPrincipalContext.set(principal.get());
            TenantContext.setTenantId(principal.get().tenantId());
            filterChain.doFilter(request, response);
        } finally {
            AdminPrincipalContext.clear();
        }
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
