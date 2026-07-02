package com.yanxitong.invitation.dto;

import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.template.entity.InvitationTemplate;
import java.time.LocalDateTime;
import java.util.Map;

public record AdminInvitationSummary(
        Invitation invitation,
        Banquet banquet,
        InvitationTemplate template,
        Map<String, String> basicFields,
        String shareUrl,
        long visitCount,
        LocalDateTime lastVisitedAt,
        boolean templateAvailable,
        String templateWarning
) {
}
