package com.yanxitong.operationlog.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.PageResult;
import com.yanxitong.operationlog.entity.OperationLog;
import com.yanxitong.operationlog.mapper.OperationLogMapper;
import com.yanxitong.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operation-logs")
public class AdminOperationLogController {
    private final OperationLogMapper mapper;

    public AdminOperationLogController(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<PageResult<OperationLog>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        QueryWrapper<OperationLog> countQuery = query(module, action, targetType, targetId, keyword);
        long total = mapper.selectCount(countQuery);
        QueryWrapper<OperationLog> query = query(module, action, targetType, targetId, keyword);
        int normalizedPageSize = PageResult.normalizePageSize(pageSize);
        query.orderByDesc("created_at").last("LIMIT " + PageResult.offset(page, pageSize) + ", " + normalizedPageSize);
        return ApiResponse.ok(PageResult.of(mapper.selectList(query), total, page, pageSize));
    }

    private QueryWrapper<OperationLog> query(String module, String action, String targetType, Long targetId, String keyword) {
        QueryWrapper<OperationLog> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        if (module != null && !module.isBlank()) {
            query.eq("module", module);
        }
        if (action != null && !action.isBlank()) {
            query.like("action", action);
        }
        if (targetType != null && !targetType.isBlank()) {
            query.eq("target_type", targetType);
        }
        if (targetId != null) {
            query.eq("target_id", targetId);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like("summary", keyword)
                    .or()
                    .like("detail", keyword)
                    .or()
                    .like("action", keyword)
                    .or()
                    .like("target_type", keyword));
        }
        return query;
    }
}
