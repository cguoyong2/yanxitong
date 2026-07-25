package com.yanxitong.invitation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.PageResult;
import com.yanxitong.device.entity.DeviceOrder;
import com.yanxitong.device.mapper.DeviceOrderMapper;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.gift.mapper.GiftRecordMapper;
import com.yanxitong.invitation.InvitationService;
import com.yanxitong.invitation.dto.AdminInvitationAnalytics;
import com.yanxitong.invitation.dto.AdminInvitationSummary;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.invitation.entity.InvitationShare;
import com.yanxitong.invitation.entity.InvitationVisitLog;
import com.yanxitong.invitation.mapper.InvitationMapper;
import com.yanxitong.invitation.mapper.InvitationShareMapper;
import com.yanxitong.invitation.mapper.InvitationVisitLogMapper;
import com.yanxitong.rsvp.entity.RsvpRecord;
import com.yanxitong.rsvp.mapper.RsvpRecordMapper;
import com.yanxitong.template.entity.InvitationTemplate;
import com.yanxitong.template.mapper.InvitationTemplateMapper;
import com.yanxitong.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final InvitationShareMapper invitationShareMapper;
    private final RsvpRecordMapper rsvpRecordMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final DeviceOrderMapper deviceOrderMapper;
    private final InvitationService invitationService;

    public AdminInvitationController(
            InvitationMapper invitationMapper,
            BanquetMapper banquetMapper,
            InvitationTemplateMapper invitationTemplateMapper,
            InvitationVisitLogMapper invitationVisitLogMapper,
            InvitationShareMapper invitationShareMapper,
            RsvpRecordMapper rsvpRecordMapper,
            GiftRecordMapper giftRecordMapper,
            DeviceOrderMapper deviceOrderMapper,
            InvitationService invitationService
    ) {
        this.invitationMapper = invitationMapper;
        this.banquetMapper = banquetMapper;
        this.invitationTemplateMapper = invitationTemplateMapper;
        this.invitationVisitLogMapper = invitationVisitLogMapper;
        this.invitationShareMapper = invitationShareMapper;
        this.rsvpRecordMapper = rsvpRecordMapper;
        this.giftRecordMapper = giftRecordMapper;
        this.deviceOrderMapper = deviceOrderMapper;
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
        Invitation invitation = requireAdminInvitation(id);
        return ApiResponse.ok(summary(invitation));
    }

    @GetMapping("/{id}/analytics")
    public ApiResponse<AdminInvitationAnalytics> analytics(@PathVariable Long id) {
        Invitation invitation = requireAdminInvitation(id);
        List<InvitationVisitLog> visits = invitationVisitLogMapper.selectList(new QueryWrapper<InvitationVisitLog>()
                .eq("invitation_id", invitation.id)
                .orderByDesc("visited_at"));
        List<RsvpRecord> rsvps = rsvpRecordMapper.selectList(new QueryWrapper<RsvpRecord>()
                .eq("invitation_id", invitation.id)
                .orderByDesc("created_at"));
        List<GiftRecord> gifts = invitation.banquetId == null
                ? List.of()
                : giftRecordMapper.selectList(new QueryWrapper<GiftRecord>()
                        .eq("banquet_id", invitation.banquetId)
                        .orderByDesc("received_at"));
        List<DeviceOrder> deviceOrders = invitation.banquetId == null
                ? List.of()
                : deviceOrderMapper.selectList(new QueryWrapper<DeviceOrder>()
                        .eq("banquet_id", invitation.banquetId)
                        .orderByDesc("created_at"));
        List<InvitationShare> shares = invitationShareMapper.selectList(new QueryWrapper<InvitationShare>()
                .eq("invitation_id", invitation.id)
                .orderByDesc("created_at"));

        long visitCount = visits.size();
        long uniqueIpCount = visits.stream()
                .map(log -> log.ipAddress)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .distinct()
                .count();
        long rsvpGuestCount = rsvps.stream().mapToLong(row -> row.guestCount == null ? 0 : row.guestCount).sum();
        BigDecimal giftAmount = gifts.stream()
                .map(row -> row.amount == null ? BigDecimal.ZERO : row.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidDeviceOrderCount = deviceOrders.stream()
                .filter(row -> "PAID".equals(row.payStatus))
                .count();
        return ApiResponse.ok(new AdminInvitationAnalytics(
                invitation.id,
                invitation.banquetId,
                visitCount,
                uniqueIpCount,
                rsvps.size(),
                rsvpGuestCount,
                gifts.size(),
                giftAmount,
                deviceOrders.size(),
                paidDeviceOrderCount,
                rate(rsvps.size(), visitCount),
                rate(gifts.size(), visitCount),
                rate(deviceOrders.size(), visitCount),
                visitTrend(visits),
                sourceBreakdown(visits),
                shareChannelBreakdown(shares),
                rsvpBreakdown(rsvps),
                deviceBreakdown(deviceOrders),
                recentVisits(visits)
        ));
    }

    private Invitation requireAdminInvitation(Long id) {
        QueryWrapper<Invitation> query = new QueryWrapper<Invitation>().eq("id", id);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            query.isNull("tenant_id");
        } else {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        Invitation invitation = invitationMapper.selectOne(query.last("LIMIT 1"));
        if (invitation == null) {
            throw new IllegalArgumentException("Invitation not found");
        }
        return invitation;
    }

    private QueryWrapper<Invitation> query(Long banquetId, Long templateId, String status, String keyword) {
        QueryWrapper<Invitation> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
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

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }

    private List<AdminInvitationAnalytics.TrendPoint> visitTrend(List<InvitationVisitLog> visits) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, Long> grouped = visits.stream()
                .filter(log -> log.visitedAt != null)
                .collect(Collectors.groupingBy(log -> log.visitedAt.toLocalDate().format(formatter), Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AdminInvitationAnalytics.TrendPoint(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<AdminInvitationAnalytics.BreakdownItem> sourceBreakdown(List<InvitationVisitLog> visits) {
        return countBy(visits, log -> sourceOf(log.userAgent));
    }

    private List<AdminInvitationAnalytics.BreakdownItem> shareChannelBreakdown(List<InvitationShare> shares) {
        return countBy(shares, share -> share.shareChannel == null || share.shareChannel.isBlank() ? "UNKNOWN" : share.shareChannel);
    }

    private List<AdminInvitationAnalytics.BreakdownItem> rsvpBreakdown(List<RsvpRecord> rsvps) {
        return countBy(rsvps, row -> row.attendanceStatus == null || row.attendanceStatus.isBlank() ? "UNKNOWN" : row.attendanceStatus);
    }

    private List<AdminInvitationAnalytics.BreakdownItem> deviceBreakdown(List<DeviceOrder> orders) {
        return countBy(orders, row -> row.deviceType == null || row.deviceType.isBlank() ? "UNKNOWN" : row.deviceType);
    }

    private <T> List<AdminInvitationAnalytics.BreakdownItem> countBy(List<T> rows, Function<T, String> classifier) {
        return rows.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new AdminInvitationAnalytics.BreakdownItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<AdminInvitationAnalytics.RecentVisit> recentVisits(List<InvitationVisitLog> visits) {
        return visits.stream()
                .limit(20)
                .map(log -> new AdminInvitationAnalytics.RecentVisit(
                        log.visitedAt == null ? "" : log.visitedAt.toString(),
                        log.ipAddress,
                        sourceOf(log.userAgent),
                        log.userAgent
                ))
                .toList();
    }

    private String sourceOf(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "UNKNOWN";
        }
        String value = userAgent.toLowerCase();
        if (value.contains("micromessenger")) {
            return "WECHAT";
        }
        if (value.contains("iphone") || value.contains("android") || value.contains("mobile")) {
            return "MOBILE_BROWSER";
        }
        return "DESKTOP_BROWSER";
    }
}
