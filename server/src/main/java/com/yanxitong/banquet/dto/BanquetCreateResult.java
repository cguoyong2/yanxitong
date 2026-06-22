package com.yanxitong.banquet.dto;

import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.invitation.entity.Invitation;

public record BanquetCreateResult(Banquet banquet, Invitation invitation, String shareUrl) {
}

