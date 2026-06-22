package com.yanxitong.banquet.controller;

import com.yanxitong.banquet.BanquetService;
import com.yanxitong.banquet.dto.BanquetDetailResult;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/banquets")
public class AdminBanquetController {
    private final BanquetService banquetService;

    public AdminBanquetController(BanquetService banquetService) {
        this.banquetService = banquetService;
    }

    @GetMapping
    public ApiResponse<List<Banquet>> list() {
        return ApiResponse.ok(banquetService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<BanquetDetailResult> detail(@PathVariable Long id) {
        return ApiResponse.ok(banquetService.detail(id));
    }
}

