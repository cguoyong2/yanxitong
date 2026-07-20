package com.yanxitong.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentMaintenanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(PaymentMaintenanceScheduler.class);

    private final PaymentMaintenanceService maintenanceService;

    public PaymentMaintenanceScheduler(PaymentMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Scheduled(
            fixedDelayString = "${payment.maintenance.fixed-delay-ms:60000}",
            initialDelayString = "${payment.maintenance.initial-delay-ms:30000}"
    )
    public void maintainPaymentOrders() {
        try {
            PaymentMaintenanceRunResult result = maintenanceService.runOnce();
            if (!result.skipped() && (result.candidates() > 0 || result.failed() > 0)) {
                log.info("Payment maintenance finished: candidates={}, paid={}, closed={}, pending={}, failed={}",
                        result.candidates(), result.paid(), result.closed(), result.pending(), result.failed());
            }
        } catch (RuntimeException ex) {
            log.error("Payment maintenance run failed", ex);
        }
    }
}
