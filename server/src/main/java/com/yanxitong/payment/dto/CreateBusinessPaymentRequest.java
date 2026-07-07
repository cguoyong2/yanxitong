package com.yanxitong.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateBusinessPaymentRequest {
    @NotBlank
    public String payerOpenId;
}
