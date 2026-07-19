package com.yanxitong.miniapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanxitong.tenant.TenantContext;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class MiniappAuthInterceptorTests {
    @AfterEach
    void tearDown() {
        MiniappPrincipalContext.clear();
        TenantContext.clear();
    }

    @Test
    void protectedHandlerRejectsMissingSession() throws Exception {
        MiniappTokenService tokenService = mock(MiniappTokenService.class);
        when(tokenService.resolve("")).thenReturn(Optional.empty());
        MiniappAuthInterceptor interceptor = new MiniappAuthInterceptor(tokenService, new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest(),
                response,
                new HandlerMethod(new SecuredHandler(), "secured")
        );

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("请先登录"));
    }

    @Test
    void validSessionSetsAndClearsUserAndTenantContexts() throws Exception {
        MiniappTokenService tokenService = mock(MiniappTokenService.class);
        MiniappPrincipal principal = new MiniappPrincipal(7L, 9L, "openid-7", "USER");
        when(tokenService.resolve("token-7")).thenReturn(Optional.of(principal));
        MiniappAuthInterceptor interceptor = new MiniappAuthInterceptor(tokenService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-7");

        assertTrue(interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                new HandlerMethod(new SecuredHandler(), "secured")
        ));
        assertEquals(7L, MiniappPrincipalContext.currentUserId());
        assertEquals(9L, TenantContext.getTenantId());

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertNull(MiniappPrincipalContext.currentUserId());
    }

    private static class SecuredHandler {
        @MiniappAuthenticated
        public void secured() {
        }
    }
}
