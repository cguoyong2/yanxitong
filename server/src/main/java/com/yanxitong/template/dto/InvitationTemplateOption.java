package com.yanxitong.template.dto;

import com.yanxitong.invitation.dto.TemplatePresentation;
import java.math.BigDecimal;

public record InvitationTemplateOption(
        Long id,
        String templateCode,
        String typeCode,
        String name,
        String coverUrl,
        String priceType,
        BigDecimal price,
        Integer sortOrder,
        String status,
        TemplatePresentation presentation
) {
}
