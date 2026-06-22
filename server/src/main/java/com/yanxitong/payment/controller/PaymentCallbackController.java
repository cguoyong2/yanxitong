package com.yanxitong.payment.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.payment.PaymentCallbackEnvelope;
import com.yanxitong.payment.PaymentCallbackService;
import com.yanxitong.payment.PaymentProvider;
import com.yanxitong.payment.dto.PaymentCallbackRequest;
import com.yanxitong.payment.entity.PaymentCallbackLog;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentCallbackController {
    private final PaymentCallbackService paymentCallbackService;

    public PaymentCallbackController(PaymentCallbackService paymentCallbackService) {
        this.paymentCallbackService = paymentCallbackService;
    }

    @PostMapping("/callbacks")
    public ApiResponse<PaymentCallbackLog> callback(@Valid @RequestBody PaymentCallbackRequest request) {
        PaymentProvider provider = PaymentProvider.valueOf(request.provider);
        return ApiResponse.ok(paymentCallbackService.handleProviderCallback(provider, request.rawBody, request.signature));
    }

    @PostMapping("/callbacks/wechat-service-provider")
    public ApiResponse<PaymentCallbackLog> wechatServiceProviderCallback(
            @RequestBody String rawBody,
            @RequestHeader Map<String, String> headers
    ) {
        PaymentCallbackEnvelope envelope = new PaymentCallbackEnvelope(
                PaymentProvider.WECHAT_SERVICE_PROVIDER,
                rawBody,
                headers,
                firstPresent(headers, "Wechatpay-Signature"),
                firstPresent(headers, "Wechatpay-Request-Id")
        );
        return ApiResponse.ok(paymentCallbackService.handleProviderCallback(envelope));
    }

    private String firstPresent(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }
}
