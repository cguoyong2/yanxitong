package com.yanxitong.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.auth.dto.LoginRequest;
import com.yanxitong.auth.dto.LoginResult;
import com.yanxitong.auth.entity.AdminUser;
import com.yanxitong.auth.mapper.AdminUserMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
    private final AdminUserMapper adminUserMapper;
    private final AuthTokenService tokenService;
    private final OperationLogService operationLogService;

    public AdminAuthService(AdminUserMapper adminUserMapper, AuthTokenService tokenService,
            OperationLogService operationLogService) {
        this.adminUserMapper = adminUserMapper;
        this.tokenService = tokenService;
        this.operationLogService = operationLogService;
    }

    public LoginResult login(LoginRequest request) {
        AdminUser user = adminUserMapper.selectOne(new QueryWrapper<AdminUser>()
                .eq("username", request.username)
                .eq("status", "ACTIVE")
                .last("LIMIT 1"));
        if (user == null || !PasswordHash.matches(request.password, user.passwordHash)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        user.lastLoginAt = LocalDateTime.now();
        adminUserMapper.updateById(user);

        AdminPrincipal principal = new AdminPrincipal(user.id, user.tenantId, user.username, user.displayName);
        String token = tokenService.issue(principal);
        AdminPrincipalContext.set(principal);
        operationLogService.record(OperationModule.AUTH, "LOGIN", "admin_user", user.id, "admin login");
        AdminPrincipalContext.clear();
        return new LoginResult(token, user.id, user.tenantId, user.username, user.displayName);
    }
}
