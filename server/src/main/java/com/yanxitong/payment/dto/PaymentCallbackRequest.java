package com.yanxitong.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class PaymentCallbackRequest {
    @NotBlank
    public String provider;
    @NotBlank
    public String rawBody;
    public String signature;
}
