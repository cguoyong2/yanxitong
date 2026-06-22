package com.yanxitong.payment.dto;

import java.util.List;

public record PaymentLaunchReadiness(
        String provider,
        boolean ready,
        List<String> blockers,
        List<ReadinessGroup> groups,
        List<ReadinessItem> checklist,
        List<String> merchantInformation,
        List<String> rolloutSteps,
        List<String> rollbackSteps
) {
    public record ReadinessItem(
            String code,
            String label,
            boolean passed,
            String detail
    ) {
    }

    public record ReadinessGroup(
            String code,
            String label,
            boolean ready,
            List<String> blockers,
            List<ReadinessItem> items
    ) {
    }
}
