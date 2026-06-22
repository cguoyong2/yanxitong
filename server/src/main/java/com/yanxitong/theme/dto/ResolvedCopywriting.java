package com.yanxitong.theme.dto;

import com.yanxitong.theme.CopywritingPriority;

public record ResolvedCopywriting(
        String title,
        String content,
        String speakerText,
        CopywritingPriority source
) {
}

