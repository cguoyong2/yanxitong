package com.yanxitong.gift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class OfflineGiftRequest {
    @NotNull
    public Long banquetId;
    @NotBlank
    public String guestName;
    @NotNull
    @DecimalMin("0.01")
    public BigDecimal amount;
    public String blessing;
}

