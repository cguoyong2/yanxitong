package com.yanxitong.theme.dto;

public record EventTypeOption(
        String eventTypeCode,
        String name,
        String alias,
        String defaultThemeCode,
        String defaultThemeName,
        String primaryColor,
        String secondaryColor,
        String defaultCopywriting
) {
}
