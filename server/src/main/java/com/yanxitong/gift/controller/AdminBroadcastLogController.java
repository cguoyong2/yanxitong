package com.yanxitong.gift.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.PageResult;
import com.yanxitong.gift.entity.BroadcastLog;
import com.yanxitong.gift.mapper.BroadcastLogMapper;
import com.yanxitong.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/broadcast-logs")
public class AdminBroadcastLogController {
    private final BroadcastLogMapper broadcastLogMapper;

    public AdminBroadcastLogController(BroadcastLogMapper broadcastLogMapper) {
        this.broadcastLogMapper = broadcastLogMapper;
    }

    @GetMapping
    public ApiResponse<PageResult<BroadcastLog>> list(
            @RequestParam(required = false) Long banquetId,
            @RequestParam(required = false) Long giftRecordId,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        QueryWrapper<BroadcastLog> countQuery = query(banquetId, giftRecordId, deviceType, eventType, status);
        long total = broadcastLogMapper.selectCount(countQuery);
        QueryWrapper<BroadcastLog> query = query(banquetId, giftRecordId, deviceType, eventType, status);
        int normalizedPageSize = PageResult.normalizePageSize(pageSize);
        query.orderByDesc("created_at").last("LIMIT " + PageResult.offset(page, pageSize) + ", " + normalizedPageSize);
        return ApiResponse.ok(PageResult.of(broadcastLogMapper.selectList(query), total, page, pageSize));
    }

    private QueryWrapper<BroadcastLog> query(Long banquetId, Long giftRecordId, String deviceType, String eventType, String status) {
        QueryWrapper<BroadcastLog> query = new QueryWrapper<BroadcastLog>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        if (banquetId != null) {
            query.eq("banquet_id", banquetId);
        }
        if (giftRecordId != null) {
            query.eq("gift_record_id", giftRecordId);
        }
        if (deviceType != null && !deviceType.isBlank()) {
            query.eq("device_type", deviceType);
        }
        if (eventType != null && !eventType.isBlank()) {
            query.eq("event_type", eventType);
        }
        if (status != null && !status.isBlank()) {
            query.eq("status", status);
        }
        return query;
    }
}
