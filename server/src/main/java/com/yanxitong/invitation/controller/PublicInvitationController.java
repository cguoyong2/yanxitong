package com.yanxitong.invitation.controller;

import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.invitation.InvitationService;
import com.yanxitong.invitation.TemplatePresentationService;
import com.yanxitong.invitation.dto.InvitationBasicResult;
import com.yanxitong.invitation.dto.PublicInvitationResult;
import com.yanxitong.invitation.dto.UpdateInvitationBasicRequest;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.security.PublicRateLimitService;
import com.yanxitong.template.entity.InvitationTemplate;
import com.yanxitong.template.mapper.InvitationTemplateMapper;
import com.yanxitong.theme.ThemeResolutionService;
import com.yanxitong.theme.entity.Theme;
import com.yanxitong.theme.mapper.ThemeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class PublicInvitationController {
    private final InvitationService invitationService;
    private final BanquetMapper banquetMapper;
    private final InvitationTemplateMapper invitationTemplateMapper;
    private final ThemeMapper themeMapper;
    private final ThemeResolutionService themeResolutionService;
    private final TemplatePresentationService templatePresentationService;
    private final PublicRateLimitService publicRateLimitService;

    public PublicInvitationController(
            InvitationService invitationService,
            BanquetMapper banquetMapper,
            InvitationTemplateMapper invitationTemplateMapper,
            ThemeMapper themeMapper,
            ThemeResolutionService themeResolutionService,
            TemplatePresentationService templatePresentationService,
            PublicRateLimitService publicRateLimitService
    ) {
        this.invitationService = invitationService;
        this.banquetMapper = banquetMapper;
        this.invitationTemplateMapper = invitationTemplateMapper;
        this.themeMapper = themeMapper;
        this.themeResolutionService = themeResolutionService;
        this.templatePresentationService = templatePresentationService;
        this.publicRateLimitService = publicRateLimitService;
    }

    @GetMapping("/public/{shareSlug}")
    public ApiResponse<PublicInvitationResult> publicView(@PathVariable String shareSlug, HttpServletRequest request) {
        publicRateLimitService.check(request, "invitation-public-view", 120, Duration.ofMinutes(1), shareSlug);
        Invitation invitation;
        try {
            invitation = invitationService.requireByShareSlug(shareSlug);
        } catch (IllegalArgumentException ex) {
            publicRateLimitService.check(request, "invitation-public-missing", 12, Duration.ofMinutes(5), shareSlug);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "请柬不存在或已失效");
        }
        invitationService.recordVisit(invitation, request);
        Banquet banquet = banquetMapper.selectById(invitation.banquetId);
        if (banquet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "宴席不存在或已失效");
        }
        Theme theme = themeMapper.selectOne(new QueryWrapper<Theme>()
                .eq("theme_code", banquet.themeCode)
                .last("LIMIT 1"));
        InvitationTemplate rawTemplate = invitation.templateId == null
                ? null
                : invitationTemplateMapper.selectById(invitation.templateId);
        boolean templateAvailable = rawTemplate == null
                ? invitation.templateId == null
                : "ACTIVE".equals(rawTemplate.status);
        InvitationTemplate template = templateAvailable ? rawTemplate : null;
        String shareUrl = "/pages/invite/public/index?slug=" + invitation.shareSlug;
        return ApiResponse.ok(new PublicInvitationResult(
                invitation,
                banquet,
                template,
                templatePresentationService.resolve(template, banquet.eventTypeCode),
                theme,
                themeResolutionService.resolveGiftSuccess(banquet.customCopywriting, banquet.themeCode, banquet.eventTypeCode),
                invitationService.parseBasicFields(invitation),
                shareUrl,
                Map.of(
                        "rsvp", "/pages/rsvp/submit/index?banquetId=" + banquet.id + "&invitationId=" + invitation.id,
                        "onlineGift", "/pages/gift/pay/index?banquetId=" + banquet.id + "&entrySource=ONLINE_GIFT",
                        "onsiteGift", "/pages/gift/pay/index?banquetId=" + banquet.id + "&entrySource=ONSITE_QR",
                        "device", "/pages/device/select/index?banquetId=" + banquet.id
                ),
                templateAvailable,
                templateAvailable ? "" : "原请柬模板已下架，当前使用基础样式展示"
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<InvitationBasicResult> detail(@PathVariable Long id) {
        Invitation invitation = invitationService.requireById(id);
        return ApiResponse.ok(new InvitationBasicResult(
                invitation,
                invitationService.parseBasicFields(invitation),
                "/pages/invite/public/index?slug=" + invitation.shareSlug
        ));
    }

    @PutMapping("/{id}/basic")
    public ApiResponse<Invitation> updateBasic(@PathVariable Long id, @Valid @RequestBody UpdateInvitationBasicRequest request) {
        return ApiResponse.ok(invitationService.updateBasic(id, request));
    }
}
