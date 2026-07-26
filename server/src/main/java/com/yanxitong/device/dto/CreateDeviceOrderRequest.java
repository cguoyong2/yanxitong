package com.yanxitong.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class CreateDeviceOrderRequest {
    @NotNull
    public Long banquetId;
    @NotBlank
    public String deviceType;
    @NotNull(message = "请选择设备租用开始时间")
    public LocalDateTime rentStartAt;
    @NotNull(message = "请选择设备租用结束时间")
    public LocalDateTime rentEndAt;
    @NotBlank
    public String deliveryMethod;
}
