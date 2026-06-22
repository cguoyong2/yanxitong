package com.yanxitong.auth.controller;

import com.yanxitong.auth.AdminAuthService;
import com.yanxitong.auth.dto.LoginRequest;
import com.yanxitong.auth.dto.LoginResult;
import com.yanxitong.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminAuthService authService;

    public AuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
