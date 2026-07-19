package com.yanxitong.miniapp;

public record LegacyOwnershipClaimResult(
        Long userId,
        int banquetCount,
        int banquetMemberCount,
        int favorContactCount,
        int familyBookCount
) {
}
