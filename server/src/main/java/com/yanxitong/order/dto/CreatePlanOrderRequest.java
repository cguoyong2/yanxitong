package com.yanxitong.order.dto;

import jakarta.validation.constraints.NotNull;

public class CreatePlanOrderRequest {
    @NotNull
    public Long banquetId;
    @NotNull
    public Long planId;
}

