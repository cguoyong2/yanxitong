package com.yanxitong.common;

import java.util.List;

public record SecurityReadinessResult(
        String status,
        boolean productionReady,
        String environment,
        List<String> activeProfiles,
        List<String> blockers,
        List<String> warnings,
        List<Item> items
) {
    public record Item(
            String code,
            String label,
            boolean passed,
            String severity,
            String detail
    ) {
    }
}
