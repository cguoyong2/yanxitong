package com.yanxitong.gift.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.PageResult;
import com.yanxitong.gift.GiftService;
import com.yanxitong.gift.dto.OfflineGiftRequest;
import com.yanxitong.gift.entity.GiftRecord;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/gifts")
public class AdminGiftController {
    private final GiftService giftService;

    public AdminGiftController(GiftService giftService) {
        this.giftService = giftService;
    }

    @GetMapping
    public ApiResponse<PageResult<GiftRecord>> list(
            @RequestParam(required = false) Long banquetId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(giftService.adminPage(banquetId, source, keyword, page, pageSize));
    }

    @PostMapping("/offline")
    public ApiResponse<GiftRecord> offline(@Valid @RequestBody OfflineGiftRequest request) {
        return ApiResponse.ok(giftService.offlineGift(request));
    }
}
