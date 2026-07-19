package com.yanxitong.favor.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.banquet.BanquetAccessService;
import com.yanxitong.favor.FavorService;
import com.yanxitong.favor.dto.FavorContactSummary;
import com.yanxitong.favor.dto.FavorDetailResult;
import com.yanxitong.favor.dto.FavorManualEntryRequest;
import com.yanxitong.favor.entity.FavorEntry;
import com.yanxitong.miniapp.MiniappAuthenticated;
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
@MiniappAuthenticated
public class FavorController {
    private final FavorService favorService;
    private final BanquetAccessService banquetAccessService;

    public FavorController(FavorService favorService, BanquetAccessService banquetAccessService) {
        this.favorService = favorService;
        this.banquetAccessService = banquetAccessService;
    }

    @GetMapping("/contacts")
    public ApiResponse<List<FavorContactSummary>> contacts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long banquetId) {
        return ApiResponse.ok(favorService.contacts(keyword, banquetId));
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
        if (request.banquetId != null) {
            banquetAccessService.requireAccessible(request.banquetId);
        }
        return ApiResponse.ok(favorService.manualEntry(request));
    }
}
