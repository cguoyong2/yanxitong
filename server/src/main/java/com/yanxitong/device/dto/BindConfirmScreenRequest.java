package com.yanxitong.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BindConfirmScreenRequest {
    @NotNull
    public Long banquetId;
    @NotBlank
    public String bindCode;
}

