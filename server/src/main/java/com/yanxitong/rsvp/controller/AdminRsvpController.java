package com.yanxitong.rsvp.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.PageResult;
import com.yanxitong.rsvp.RsvpService;
import com.yanxitong.rsvp.dto.RsvpStatsResult;
import com.yanxitong.rsvp.entity.RsvpRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rsvp")
public class AdminRsvpController {
    private final RsvpService rsvpService;

    public AdminRsvpController(RsvpService rsvpService) {
        this.rsvpService = rsvpService;
    }

    @GetMapping
    public ApiResponse<PageResult<RsvpRecord>> list(
            @RequestParam(required = false) Long banquetId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(rsvpService.adminPage(banquetId, status, keyword, page, pageSize));
    }

    @GetMapping("/stats")
    public ApiResponse<RsvpStatsResult> stats(@RequestParam Long banquetId) {
        return ApiResponse.ok(rsvpService.stats(banquetId));
    }
}
