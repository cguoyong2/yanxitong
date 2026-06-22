package com.yanxitong.favor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FavorManualEntryRequest {
    @NotBlank
    public String contactName;
    public String phone;
    public Long banquetId;
    @NotBlank
    public String direction;
    @NotNull
    @DecimalMin("0.01")
    public BigDecimal amount;
    public LocalDateTime occurredAt;
    public String note;
}

