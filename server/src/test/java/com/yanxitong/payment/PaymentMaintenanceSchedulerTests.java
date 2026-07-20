package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class PaymentMaintenanceSchedulerTests {
    @Test
    void providerFailureDoesNotEscapeScheduledTask() {
        PaymentMaintenanceService service = mock(PaymentMaintenanceService.class);
        when(service.runOnce()).thenThrow(new IllegalStateException("provider unavailable"));
        PaymentMaintenanceScheduler scheduler = new PaymentMaintenanceScheduler(service);

        assertDoesNotThrow(scheduler::maintainPaymentOrders);
    }
}
