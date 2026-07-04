package com.yanxitong.favor.dto;

import com.yanxitong.favor.entity.FavorFamilyBook;
import com.yanxitong.favor.entity.FavorFamilyMember;
import java.math.BigDecimal;
import java.util.List;

public record FamilyBookSummary(
        FavorFamilyBook book,
        List<FavorFamilyMember> members,
        BigDecimal receivedAmount,
        BigDecimal givenAmount,
        BigDecimal balance,
        long contactCount
) {
}
