package com.yanxitong.config.dto;

public record CustomerServiceConfig(String acquireLink, boolean enabled) {
    public static CustomerServiceConfig disabled() {
        return new CustomerServiceConfig("", false);
    }
}
