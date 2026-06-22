package com.yanxitong.device.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDeviceOrderStatusRequest(
        @NotBlank String orderStatus
) {
}
