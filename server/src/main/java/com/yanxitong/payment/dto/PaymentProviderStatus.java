package com.yanxitong.payment.dto;

import java.util.List;

public record PaymentProviderStatus(
        String provider,
        boolean defaultProvider,
        boolean enabled,
        boolean productionReady,
        boolean callbackSecretConfigured,
        boolean apiV3KeyConfigured,
        boolean privateKeyConfigured,
        boolean notifyUrlConfigured,
        String certificateMode,
        String merchantId,
        String appId,
        String serviceProviderId,
        String subMerchantId,
        String subAppId,
        String certificateSerialNo,
        List<String> requiredItems,
        List<String> missingItems
) {
}
