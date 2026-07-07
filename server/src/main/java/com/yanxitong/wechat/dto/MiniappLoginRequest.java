package com.yanxitong.wechat.dto;

import jakarta.validation.constraints.NotBlank;

public class MiniappLoginRequest {
    @NotBlank
    public String code;
}
