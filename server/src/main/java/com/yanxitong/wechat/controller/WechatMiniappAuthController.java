package com.yanxitong.wechat.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.wechat.WechatMiniappAuthService;
import com.yanxitong.wechat.dto.MiniappLoginRequest;
import com.yanxitong.wechat.dto.MiniappOpenIdResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wechat/miniapp")
public class WechatMiniappAuthController {
    private final WechatMiniappAuthService authService;

    public WechatMiniappAuthController(WechatMiniappAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/openid")
    public ApiResponse<MiniappOpenIdResult> openId(@Valid @RequestBody MiniappLoginRequest request) {
        return ApiResponse.ok(new MiniappOpenIdResult(authService.resolveOpenId(request.code)));
    }
}
