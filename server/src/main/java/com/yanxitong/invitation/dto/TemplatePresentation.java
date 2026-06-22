package com.yanxitong.invitation.dto;

public record TemplatePresentation(
        String styleCode,
        String headline,
        String defaultGreeting,
        String defaultScheduleText,
        String fallbackCoverLabel
) {
}
