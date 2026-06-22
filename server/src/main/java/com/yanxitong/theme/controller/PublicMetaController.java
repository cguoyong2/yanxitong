package com.yanxitong.theme.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.invitation.TemplatePresentationService;
import com.yanxitong.template.dto.InvitationTemplateOption;
import com.yanxitong.template.entity.InvitationTemplate;
import com.yanxitong.template.mapper.InvitationTemplateMapper;
import com.yanxitong.theme.dto.EventTypeOption;
import com.yanxitong.theme.entity.EventType;
import com.yanxitong.theme.entity.Theme;
import com.yanxitong.theme.mapper.EventTypeMapper;
import com.yanxitong.theme.mapper.ThemeMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
public class PublicMetaController {
    private final EventTypeMapper eventTypeMapper;
    private final ThemeMapper themeMapper;
    private final InvitationTemplateMapper invitationTemplateMapper;
    private final TemplatePresentationService templatePresentationService;

    public PublicMetaController(EventTypeMapper eventTypeMapper, ThemeMapper themeMapper,
            InvitationTemplateMapper invitationTemplateMapper,
            TemplatePresentationService templatePresentationService) {
        this.eventTypeMapper = eventTypeMapper;
        this.themeMapper = themeMapper;
        this.invitationTemplateMapper = invitationTemplateMapper;
        this.templatePresentationService = templatePresentationService;
    }

    @GetMapping("/event-types")
    public ApiResponse<List<EventTypeOption>> eventTypes() {
        List<EventType> eventTypes = eventTypeMapper.selectList(new QueryWrapper<EventType>()
                .eq("enabled", 1)
                .orderByAsc("sort_order"));
        return ApiResponse.ok(eventTypes.stream().map(this::toOption).toList());
    }

    @GetMapping("/invitation-templates")
    public ApiResponse<List<InvitationTemplateOption>> templates() {
        List<InvitationTemplate> templates = invitationTemplateMapper.selectList(new QueryWrapper<InvitationTemplate>()
                .eq("status", "ACTIVE")
                .orderByAsc("sort_order"));
        return ApiResponse.ok(templates.stream().map(template -> new InvitationTemplateOption(
                template.id,
                template.templateCode,
                template.typeCode,
                template.name,
                template.coverUrl,
                template.priceType,
                template.price,
                template.sortOrder,
                template.status,
                templatePresentationService.resolve(template, null)
        )).toList());
    }

    private EventTypeOption toOption(EventType eventType) {
        Theme theme = themeMapper.selectOne(new QueryWrapper<Theme>()
                .eq("theme_code", eventType.defaultThemeCode)
                .eq("enabled", 1)
                .last("LIMIT 1"));
        return new EventTypeOption(
                eventType.eventTypeCode,
                eventType.name,
                eventType.alias,
                eventType.defaultThemeCode,
                theme == null ? eventType.defaultThemeCode : theme.name,
                theme == null ? null : theme.primaryColor,
                theme == null ? null : theme.secondaryColor,
                eventType.defaultCopywriting
        );
    }
}
