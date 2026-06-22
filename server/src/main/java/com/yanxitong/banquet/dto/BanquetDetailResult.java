package com.yanxitong.banquet.dto;

import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.theme.dto.ResolvedCopywriting;
import com.yanxitong.theme.entity.Theme;

public record BanquetDetailResult(
        Banquet banquet,
        Invitation invitation,
        Theme theme,
        ResolvedCopywriting giftSuccessCopywriting
) {
}

