package com.yanxitong.banquet.controller;

import com.yanxitong.banquet.BanquetService;
import com.yanxitong.banquet.dto.BanquetCreateResult;
import com.yanxitong.banquet.dto.BanquetDetailResult;
import com.yanxitong.banquet.dto.CreateBanquetRequest;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banquets")
public class BanquetController {
    private final BanquetService banquetService;

    public BanquetController(BanquetService banquetService) {
        this.banquetService = banquetService;
    }

    @GetMapping
    public ApiResponse<List<Banquet>> list() {
        return ApiResponse.ok(banquetService.list());
    }

    @PostMapping
    public ApiResponse<BanquetCreateResult> create(@Valid @RequestBody CreateBanquetRequest request) {
        return ApiResponse.ok(banquetService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<BanquetDetailResult> detail(@PathVariable Long id) {
        return ApiResponse.ok(banquetService.detail(id));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<BanquetDetailResult> publish(@PathVariable Long id) {
        return ApiResponse.ok(banquetService.publish(id));
    }
}
