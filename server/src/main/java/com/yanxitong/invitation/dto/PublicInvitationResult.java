package com.yanxitong.invitation.dto;

import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.template.entity.InvitationTemplate;
import com.yanxitong.theme.dto.ResolvedCopywriting;
import com.yanxitong.theme.entity.Theme;
import java.util.Map;

public record PublicInvitationResult(
        Invitation invitation,
        Banquet banquet,
        InvitationTemplate template,
        TemplatePresentation templatePresentation,
        Theme theme,
        ResolvedCopywriting giftSuccessCopywriting,
        Map<String, String> basicFields,
        String shareUrl,
        Map<String, String> actionUrls
) {
}
