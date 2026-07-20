package com.yanxitong.payment;

public record PaymentMaintenanceRunResult(
        int candidates,
        int paid,
        int closed,
        int pending,
        int failed,
        boolean skipped
) {
    public static PaymentMaintenanceRunResult skippedRun() {
        return new PaymentMaintenanceRunResult(0, 0, 0, 0, 0, true);
    }
}
