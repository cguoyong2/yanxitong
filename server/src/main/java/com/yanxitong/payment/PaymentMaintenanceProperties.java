package com.yanxitong.payment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment.maintenance")
public class PaymentMaintenanceProperties {
    private boolean enabled;
    private Duration queryAfter = Duration.ofMinutes(1);
    private Duration pendingTimeout = Duration.ofMinutes(30);
    private Duration retryDelay = Duration.ofMinutes(2);
    private int batchSize = 50;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getQueryAfter() {
        return queryAfter;
    }

    public void setQueryAfter(Duration queryAfter) {
        this.queryAfter = positiveDuration(queryAfter, Duration.ofMinutes(1));
    }

    public Duration getPendingTimeout() {
        return pendingTimeout;
    }

    public void setPendingTimeout(Duration pendingTimeout) {
        this.pendingTimeout = positiveDuration(pendingTimeout, Duration.ofMinutes(30));
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = positiveDuration(retryDelay, Duration.ofMinutes(2));
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, Math.min(batchSize, 200));
    }

    private Duration positiveDuration(Duration value, Duration fallback) {
        return value == null || value.isNegative() || value.isZero() ? fallback : value;
    }
}
