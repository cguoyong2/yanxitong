package com.yanxitong.favor.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.favor.FavorService;
import com.yanxitong.favor.dto.FavorContactSummary;
import com.yanxitong.favor.dto.FavorDetailResult;
import com.yanxitong.favor.dto.FavorManualEntryRequest;
import com.yanxitong.favor.entity.FavorEntry;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favor")
public class FavorController {
    private final FavorService favorService;

    public FavorController(FavorService favorService) {
        this.favorService = favorService;
    }

    @GetMapping("/contacts")
    public ApiResponse<List<FavorContactSummary>> contacts(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(favorService.contacts(keyword));
    }

    @GetMapping("/contacts/{id}")
    public ApiResponse<FavorDetailResult> detail(@PathVariable Long id) {
        return ApiResponse.ok(favorService.detail(id));
    }

    @GetMapping("/compare")
    public ApiResponse<FavorDetailResult> compare(@RequestParam String contactName) {
        return ApiResponse.ok(favorService.compareByName(contactName));
    }

    @PostMapping("/manual")
    public ApiResponse<FavorEntry> manual(@Valid @RequestBody FavorManualEntryRequest request) {
        return ApiResponse.ok(favorService.manualEntry(request));
    }
}
