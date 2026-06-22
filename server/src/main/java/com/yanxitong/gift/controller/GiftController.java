package com.yanxitong.gift.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.gift.GiftService;
import com.yanxitong.gift.dto.CreateGiftPaymentRequest;
import com.yanxitong.gift.dto.GiftPaymentOrderResult;
import com.yanxitong.gift.dto.GiftSummaryResult;
import com.yanxitong.gift.dto.OfflineGiftRequest;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.payment.MockPaymentGuard;
import com.yanxitong.payment.PaymentCallbackService;
import com.yanxitong.security.PublicRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gifts")
public class GiftController {
    private final GiftService giftService;
    private final PaymentCallbackService paymentCallbackService;
    private final MockPaymentGuard mockPaymentGuard;
    private final PublicRateLimitService publicRateLimitService;

    public GiftController(
            GiftService giftService,
            PaymentCallbackService paymentCallbackService,
            MockPaymentGuard mockPaymentGuard,
            PublicRateLimitService publicRateLimitService
    ) {
        this.giftService = giftService;
        this.paymentCallbackService = paymentCallbackService;
        this.mockPaymentGuard = mockPaymentGuard;
        this.publicRateLimitService = publicRateLimitService;
    }

    @GetMapping
    public ApiResponse<List<GiftRecord>> list(
            @RequestParam Long banquetId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(giftService.list(banquetId, source, keyword));
    }

    @GetMapping("/summary")
    public ApiResponse<GiftSummaryResult> summary(@RequestParam Long banquetId) {
        return ApiResponse.ok(giftService.summary(banquetId));
    }

    @PostMapping("/payment-orders")
    public ApiResponse<GiftPaymentOrderResult> createPaymentOrder(
            @Valid @RequestBody CreateGiftPaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        publicRateLimitService.check(
                httpRequest,
                "gift-payment-order-create",
                8,
                Duration.ofMinutes(10),
                String.valueOf(request.banquetId),
                normalize(request.entrySource),
                normalize(request.guestName)
        );
        return ApiResponse.ok(giftService.createPaymentOrder(request));
    }

    @PostMapping("/payment-orders/{orderNo}/mock-success")
    public ApiResponse<GiftRecord> mockSuccess(@PathVariable String orderNo) {
        mockPaymentGuard.requireEnabled();
        return ApiResponse.ok(paymentCallbackService.mockSuccess(orderNo));
    }

    @PostMapping("/offline")
    public ApiResponse<GiftRecord> offline(@Valid @RequestBody OfflineGiftRequest request, HttpServletRequest httpRequest) {
        publicRateLimitService.check(
                httpRequest,
                "gift-offline-create",
                20,
                Duration.ofMinutes(10),
                String.valueOf(request.banquetId),
                normalize(request.guestName)
        );
        return ApiResponse.ok(giftService.offlineGift(request));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
