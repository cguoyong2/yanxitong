package com.yanxitong.favor.dto;

import com.yanxitong.favor.entity.FavorContact;
import com.yanxitong.favor.entity.FavorEntry;
import java.math.BigDecimal;
import java.util.List;

public record FavorDetailResult(
        FavorContact contact,
        BigDecimal receivedAmount,
        BigDecimal givenAmount,
        BigDecimal balance,
        List<FavorEntry> entries
) {
}

