package com.yanxitong.theme;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.config.mapper.ConfigItemMapper;
import com.yanxitong.theme.dto.ResolvedCopywriting;
import com.yanxitong.theme.entity.EventType;
import com.yanxitong.theme.entity.ThemeCopywriting;
import com.yanxitong.theme.mapper.EventTypeMapper;
import com.yanxitong.theme.mapper.ThemeCopywritingMapper;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ThemeResolutionService {
    private static final String SYSTEM_DEFAULT_GIFT_SUCCESS = "system.default.copywriting.gift_success";

    private final EventTypeMapper eventTypeMapper;
    private final ThemeCopywritingMapper themeCopywritingMapper;
    private final ConfigItemMapper configItemMapper;
    private final ObjectMapper objectMapper;

    public ThemeResolutionService(
            EventTypeMapper eventTypeMapper,
            ThemeCopywritingMapper themeCopywritingMapper,
            ConfigItemMapper configItemMapper,
            ObjectMapper objectMapper
    ) {
        this.eventTypeMapper = eventTypeMapper;
        this.themeCopywritingMapper = themeCopywritingMapper;
        this.configItemMapper = configItemMapper;
        this.objectMapper = objectMapper;
    }

    public EventType requireEventType(String eventTypeCode) {
        EventType eventType = eventTypeMapper.selectOne(new QueryWrapper<EventType>()
                .eq("event_type_code", eventTypeCode)
                .eq("enabled", 1)
                .last("LIMIT 1"));
        if (eventType == null) {
            throw new IllegalArgumentException("Unknown event type: " + eventTypeCode);
        }
        return eventType;
    }

    public ResolvedCopywriting resolveGiftSuccess(String customCopywriting, String themeCode, String eventTypeCode) {
        if (customCopywriting != null && !customCopywriting.isBlank()) {
            Optional<ResolvedCopywriting> custom = parseCustomGiftSuccess(customCopywriting);
            if (custom.isPresent()) {
                return custom.get();
            }
            return new ResolvedCopywriting("心意已收到", customCopywriting, customCopywriting, CopywritingPriority.BANQUET_CUSTOM);
        }

        ThemeCopywriting themeCopywriting = themeCopywritingMapper.selectOne(new QueryWrapper<ThemeCopywriting>()
                .eq("theme_code", themeCode)
                .eq("event_type_code", eventTypeCode)
                .eq("scene_code", "GIFT_SUCCESS")
                .eq("enabled", 1)
                .last("LIMIT 1"));
        if (themeCopywriting != null) {
            return new ResolvedCopywriting(
                    themeCopywriting.title,
                    themeCopywriting.content,
                    themeCopywriting.speakerText,
                    CopywritingPriority.THEME_COPYWRITING
            );
        }

        EventType eventType = requireEventType(eventTypeCode);
        if (eventType.defaultCopywriting != null && !eventType.defaultCopywriting.isBlank()) {
            return new ResolvedCopywriting("心意已收到", eventType.defaultCopywriting, eventType.defaultCopywriting, CopywritingPriority.EVENT_TYPE_DEFAULT);
        }

        String systemDefault = Optional.ofNullable(configItemMapper.selectOne(new QueryWrapper<com.yanxitong.config.entity.ConfigItem>()
                        .eq("config_key", SYSTEM_DEFAULT_GIFT_SUCCESS)
                        .eq("enabled", 1)
                        .last("LIMIT 1")))
                .map(item -> item.configValue)
                .orElse("心意已收到，感谢您的祝福");
        return new ResolvedCopywriting("心意已收到", systemDefault, systemDefault, CopywritingPriority.SYSTEM_DEFAULT);
    }

    private Optional<ResolvedCopywriting> parseCustomGiftSuccess(String customCopywriting) {
        try {
            Map<String, String> values = objectMapper.readValue(customCopywriting, new TypeReference<>() {
            });
            String content = firstNonBlank(values.get("gift_success"), values.get("content"));
            if (content == null) {
                return Optional.empty();
            }
            String title = firstNonBlank(values.get("gift_success_title"), values.get("title"), "心意已收到");
            String speakerText = firstNonBlank(values.get("gift_success_speaker_text"), values.get("speaker_text"), content);
            return Optional.of(new ResolvedCopywriting(title, content, speakerText, CopywritingPriority.BANQUET_CUSTOM));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
