package com.yanxitong.common;

import com.yanxitong.payment.MockPaymentGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime")
public class RuntimeFeatureController {
    private final MockPaymentGuard mockPaymentGuard;

    public RuntimeFeatureController(MockPaymentGuard mockPaymentGuard) {
        this.mockPaymentGuard = mockPaymentGuard;
    }

    @GetMapping("/features")
    public ApiResponse<RuntimeFeatures> features() {
        return ApiResponse.ok(new RuntimeFeatures(mockPaymentGuard.isEnabled()));
    }

    public record RuntimeFeatures(boolean mockPaymentEnabled) {
    }
}
