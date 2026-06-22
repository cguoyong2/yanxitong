package com.yanxitong.device.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.device.ConfirmScreenService;
import com.yanxitong.device.dto.BindConfirmScreenRequest;
import com.yanxitong.device.dto.ConfirmScreenGiftEvent;
import com.yanxitong.device.dto.ConfirmScreenStatusResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/confirm-screen")
public class ConfirmScreenController {
    private final ConfirmScreenService confirmScreenService;

    public ConfirmScreenController(ConfirmScreenService confirmScreenService) {
        this.confirmScreenService = confirmScreenService;
    }

    @PostMapping("/bind")
    public ApiResponse<ConfirmScreenStatusResult> bind(@Valid @RequestBody BindConfirmScreenRequest request) {
        return ApiResponse.ok(confirmScreenService.bind(request));
    }

    @GetMapping("/status/{bindCode}")
    public ApiResponse<ConfirmScreenStatusResult> status(@PathVariable String bindCode) {
        return ApiResponse.ok(confirmScreenService.status(bindCode));
    }

    @GetMapping("/banquets/{banquetId}/latest-event")
    public ApiResponse<ConfirmScreenGiftEvent> latestEvent(@PathVariable Long banquetId) {
        return ApiResponse.ok(confirmScreenService.latestEvent(banquetId));
    }
}
