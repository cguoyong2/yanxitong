package com.yanxitong.wechat.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.miniapp.MiniappLoginService;
import com.yanxitong.miniapp.dto.MiniappSessionResult;
import com.yanxitong.wechat.dto.MiniappLoginRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wechat/miniapp")
public class WechatMiniappAuthController {
    private final MiniappLoginService loginService;

    public WechatMiniappAuthController(MiniappLoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ApiResponse<MiniappSessionResult> login(@Valid @RequestBody MiniappLoginRequest request) {
        return ApiResponse.ok(loginService.login(request.code));
    }

}
