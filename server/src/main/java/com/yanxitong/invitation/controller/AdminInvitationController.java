package com.yanxitong.invitation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.PageResult;
import com.yanxitong.invitation.InvitationService;
import com.yanxitong.invitation.dto.AdminInvitationSummary;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.invitation.entity.InvitationVisitLog;
import com.yanxitong.invitation.mapper.InvitationMapper;
import com.yanxitong.invitation.mapper.InvitationVisitLogMapper;
import com.yanxitong.template.entity.InvitationTemplate;
import com.yanxitong.template.mapper.InvitationTemplateMapper;
import com.yanxitong.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/invitations")
public class AdminInvitationController {
    private final InvitationMapper invitationMapper;
    private final BanquetMapper banquetMapper;
    private final InvitationTemplateMapper invitationTemplateMapper;
    private final InvitationVisitLogMapper invitationVisitLogMapper;
    private final InvitationService invitationService;

    public AdminInvitationController(
            InvitationMapper invitationMapper,
            BanquetMapper banquetMapper,
            InvitationTemplateMapper invitationTemplateMapper,
            InvitationVisitLogMapper invitationVisitLogMapper,
            InvitationService invitationService
    ) {
        this.invitationMapper = invitationMapper;
        this.banquetMapper = banquetMapper;
        this.invitationTemplateMapper = invitationTemplateMapper;
        this.invitationVisitLogMapper = invitationVisitLogMapper;
        this.invitationService = invitationService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminInvitationSummary>> list(
            @RequestParam(required = false) Long banquetId,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        QueryWrapper<Invitation> countQuery = query(banquetId, templateId, status, keyword);
        long total = invitationMapper.selectCount(countQuery);
        QueryWrapper<Invitation> pageQuery = query(banquetId, templateId, status, keyword)
                .orderByDesc("updated_at")
                .last("LIMIT " + PageResult.normalizePageSize(pageSize) + " OFFSET " + PageResult.offset(page, pageSize));
        List<AdminInvitationSummary> records = invitationMapper.selectList(pageQuery).stream()
                .map(this::summary)
                .toList();
        return ApiResponse.ok(PageResult.of(records, total, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminInvitationSummary> detail(@PathVariable Long id) {
        Invitation invitation = invitationService.requireById(id);
        return ApiResponse.ok(summary(invitation));
    }

    private QueryWrapper<Invitation> query(Long banquetId, Long templateId, String status, String keyword) {
        QueryWrapper<Invitation> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.eq("tenant_id", tenantId);
        } else {
            query.isNull("tenant_id");
        }
        if (banquetId != null) {
            query.eq("banquet_id", banquetId);
        }
        if (templateId != null) {
            query.eq("template_id", templateId);
        }
        if (status != null && !status.isBlank()) {
            query.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim() + "%";
            query.and(wrapper -> wrapper.like("title", pattern).or().like("share_slug", pattern));
        }
        return query;
    }

    private AdminInvitationSummary summary(Invitation invitation) {
        Banquet banquet = invitation.banquetId == null ? null : banquetMapper.selectById(invitation.banquetId);
        InvitationTemplate template = invitation.templateId == null ? null : invitationTemplateMapper.selectById(invitation.templateId);
        long visitCount = invitationVisitLogMapper.selectCount(new QueryWrapper<InvitationVisitLog>()
                .eq("invitation_id", invitation.id));
        InvitationVisitLog latestVisit = invitationVisitLogMapper.selectOne(new QueryWrapper<InvitationVisitLog>()
                .eq("invitation_id", invitation.id)
                .orderByDesc("visited_at")
                .last("LIMIT 1"));
        LocalDateTime lastVisitedAt = latestVisit == null ? null : latestVisit.visitedAt;
        boolean templateAvailable = template == null
                ? invitation.templateId == null
                : "ACTIVE".equals(template.status);
        return new AdminInvitationSummary(
                invitation,
                banquet,
                template,
                invitationService.parseBasicFields(invitation),
                "/pages/invite/public/index?slug=" + invitation.shareSlug,
                visitCount,
                lastVisitedAt,
                templateAvailable,
                templateAvailable ? "" : "原请柬模板已下架，公开页将使用基础样式"
        );
    }
}
