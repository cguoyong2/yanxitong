package com.yanxitong.rsvp.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.rsvp.RsvpService;
import com.yanxitong.rsvp.dto.RsvpStatsResult;
import com.yanxitong.rsvp.dto.RsvpSubmitRequest;
import com.yanxitong.rsvp.entity.RsvpRecord;
import com.yanxitong.security.PublicRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rsvp")
public class RsvpController {
    private final RsvpService rsvpService;
    private final PublicRateLimitService publicRateLimitService;

    public RsvpController(RsvpService rsvpService, PublicRateLimitService publicRateLimitService) {
        this.rsvpService = rsvpService;
        this.publicRateLimitService = publicRateLimitService;
    }

    @PostMapping("/submit")
    public ApiResponse<RsvpRecord> submit(@Valid @RequestBody RsvpSubmitRequest request, HttpServletRequest httpRequest) {
        publicRateLimitService.check(
                httpRequest,
                "rsvp-submit",
                20,
                Duration.ofMinutes(10),
                String.valueOf(request.banquetId),
                normalize(request.guestName)
        );
        return ApiResponse.ok(rsvpService.submit(request));
    }

    @GetMapping("/list")
    public ApiResponse<List<RsvpRecord>> list(@RequestParam Long banquetId) {
        return ApiResponse.ok(rsvpService.list(banquetId));
    }

    @GetMapping("/stats")
    public ApiResponse<RsvpStatsResult> stats(@RequestParam Long banquetId) {
        return ApiResponse.ok(rsvpService.stats(banquetId));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
