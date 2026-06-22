package com.yanxitong.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class CreateDeviceOrderRequest {
    @NotNull
    public Long banquetId;
    @NotBlank
    public String deviceType;
    public LocalDateTime rentStartAt;
    public LocalDateTime rentEndAt;
    @NotBlank
    public String deliveryMethod;
}

