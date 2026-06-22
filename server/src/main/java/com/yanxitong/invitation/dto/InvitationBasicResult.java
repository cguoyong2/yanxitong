package com.yanxitong.invitation.dto;

import com.yanxitong.invitation.entity.Invitation;
import java.util.Map;

public record InvitationBasicResult(
        Invitation invitation,
        Map<String, String> basicFields,
        String shareUrl
) {
}
