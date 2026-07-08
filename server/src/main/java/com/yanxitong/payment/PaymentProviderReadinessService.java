package com.yanxitong.payment;

import com.yanxitong.payment.dto.PaymentLaunchReadiness;
import com.yanxitong.payment.dto.PaymentProviderStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentProviderReadinessService {
    private final PaymentProviderProperties properties;

    public PaymentProviderReadinessService(PaymentProviderProperties properties) {
        this.properties = properties;
    }

    public PaymentProviderStatus status(PaymentProvider provider) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider);
        List<String> requiredItems = requiredItems(provider, config);
        List<String> missingItems = missingItems(provider, config);
        return new PaymentProviderStatus(
                provider.name(),
                provider == properties.getDefaultProvider(),
                config.isEnabled(),
                config.isEnabled() && missingItems.isEmpty(),
                config.hasCallbackSecret(),
                config.hasApiV3Key(),
                config.hasPrivateKeyPath(),
                config.hasNotifyUrl(),
                certificateMode(config),
                mask(config.getMerchantId()),
                mask(config.getAppId()),
                mask(config.getServiceProviderId()),
                mask(config.getSubMerchantId()),
                mask(config.getSubAppId()),
                mask(config.getCertificateSerialNo()),
                requiredItems,
                missingItems
        );
    }

    public PaymentLaunchReadiness launchReadiness(PaymentProvider provider) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider);
        List<String> missingItems = missingItems(provider, config);
        List<PaymentLaunchReadiness.ReadinessItem> providerItems = List.of(
                readiness("provider-enabled", "支付通道已启用", config.isEnabled(), "payment.providers." + provider + ".enabled"),
                readiness("default-provider", "默认通道已切换", provider == properties.getDefaultProvider(), "PAYMENT_DEFAULT_PROVIDER=" + provider),
                readiness("core-config", "核心商户配置完整", missingItems.isEmpty(), missingItems.isEmpty() ? "无缺失项" : "缺失: " + String.join(", ", missingItems))
        );
        List<PaymentLaunchReadiness.ReadinessItem> callbackItems = List.of(
                readiness("notify-url", "回调地址已配置", config.hasNotifyUrl(), "notifyUrl must route to " + callbackPath(provider)),
                readiness("callback-security-material", "回调验签材料已配置", callbackSecurityReady(config), callbackSecurityDetail(config))
        );
        List<PaymentLaunchReadiness.ReadinessItem> keyItems = List.of(
                readiness("private-key", "私钥路径已配置", config.hasPrivateKeyPath(), "privateKeyPath uses filesystem or secret mounted path"),
                readiness("api-v3-key", "API v3 Key 已配置", config.hasApiV3Key(), "apiV3Key only visible through configured boolean status")
        );
        List<PaymentLaunchReadiness.ReadinessGroup> groups = List.of(
                group("provider-config", "支付通道配置", providerItems),
                group("callback-security", "回调验签与通知", callbackItems),
                group("merchant-secret", "商户密钥材料", keyItems)
        );
        List<PaymentLaunchReadiness.ReadinessItem> checklist = groups.stream()
                .flatMap(group -> group.items().stream())
                .toList();
        List<String> blockers = checklist.stream()
                .filter(item -> !item.passed())
                .map(PaymentLaunchReadiness.ReadinessItem::label)
                .toList();
        return new PaymentLaunchReadiness(
                provider.name(),
                blockers.isEmpty(),
                blockers,
                groups,
                checklist,
                merchantInformation(provider),
                rolloutSteps(provider),
                rollbackSteps()
        );
    }

    public void requireCreateReady(PaymentProvider provider) {
        if (provider == PaymentProvider.MOCK) {
            return;
        }
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider);
        List<String> missing = missingItems(provider, config);
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "支付通道配置未就绪: " + String.join(", ", missing)
            );
        }
    }

    public List<String> requiredItems(PaymentProvider provider, PaymentProviderProperties.ProviderConfig config) {
        if (provider == PaymentProvider.MOCK) {
            return List.of("enabled", "callbackSecret");
        }
        List<String> items = new ArrayList<>(List.of(
                "enabled",
                "merchantId",
                "appId",
                "certificateSerialNo",
                "privateKeyPath",
                "apiV3Key",
                "notifyUrl"
        ));
        if (provider == PaymentProvider.WECHAT_SERVICE_PROVIDER) {
            items.add("serviceProviderId");
            items.add("subMerchantId");
        }
        String mode = certificateMode(config);
        if ("PLATFORM_CERTIFICATE".equals(mode)) {
            items.add("platformCertificatePath");
        }
        if ("PUBLIC_KEY".equals(mode)) {
            items.add("wechatPayPublicKeyId");
            items.add("wechatPayPublicKeyPath");
        }
        return items;
    }

    public List<String> missingItems(PaymentProvider provider, PaymentProviderProperties.ProviderConfig config) {
        List<String> missing = new ArrayList<>();
        if (!config.isEnabled()) {
            missing.add("enabled");
        }
        if (provider == PaymentProvider.MOCK) {
            addMissing(missing, "callbackSecret", config.hasCallbackSecret());
            return missing;
        }
        addMissing(missing, "merchantId", hasText(config.getMerchantId()));
        addMissing(missing, "appId", hasText(config.getAppId()));
        if (provider == PaymentProvider.WECHAT_SERVICE_PROVIDER) {
            addMissing(missing, "serviceProviderId", hasText(config.getServiceProviderId()));
            addMissing(missing, "subMerchantId", hasText(config.getSubMerchantId()));
        }
        addMissing(missing, "certificateSerialNo", hasText(config.getCertificateSerialNo()));
        addMissing(missing, "privateKeyPath", config.hasPrivateKeyPath());
        addMissing(missing, "apiV3Key", config.hasApiV3Key());
        addMissing(missing, "notifyUrl", config.hasNotifyUrl());
        String mode = certificateMode(config);
        if ("PLATFORM_CERTIFICATE".equals(mode)) {
            addMissing(missing, "platformCertificatePath", hasText(config.getPlatformCertificatePath()));
        }
        if ("PUBLIC_KEY".equals(mode)) {
            addMissing(missing, "wechatPayPublicKeyId", hasText(config.getWechatPayPublicKeyId()));
            addMissing(missing, "wechatPayPublicKeyPath", hasText(config.getWechatPayPublicKeyPath()));
        }
        return missing;
    }

    private PaymentLaunchReadiness.ReadinessItem readiness(String code, String label, boolean passed, String detail) {
        return new PaymentLaunchReadiness.ReadinessItem(code, label, passed, detail);
    }

    private PaymentLaunchReadiness.ReadinessGroup group(
            String code,
            String label,
            List<PaymentLaunchReadiness.ReadinessItem> items
    ) {
        List<String> blockers = items.stream()
                .filter(item -> !item.passed())
                .map(PaymentLaunchReadiness.ReadinessItem::label)
                .toList();
        return new PaymentLaunchReadiness.ReadinessGroup(code, label, blockers.isEmpty(), blockers, items);
    }

    private List<String> merchantInformation(PaymentProvider provider) {
        if (provider == PaymentProvider.MOCK) {
            return List.of("mock callback secret for local acceptance");
        }
        if (provider == PaymentProvider.WECHAT_DIRECT) {
            return List.of(
                    "direct merchant id",
                    "miniapp app id and OpenID ownership",
                    "merchant certificate serial number",
                    "merchant private key file path",
                    "API v3 key",
                    "public reachable notify URL",
                    "certificate mode and platform certificate or WeChat Pay public key material"
            );
        }
        return List.of(
                "service provider merchant id",
                "service provider app id",
                "sub-merchant id",
                "sub-merchant app id and OpenID ownership",
                "merchant certificate serial number",
                "merchant private key file path",
                "API v3 key",
                "public reachable notify URL",
                "certificate mode and platform certificate or WeChat Pay public key material"
        );
    }

    private List<String> rolloutSteps(PaymentProvider provider) {
        return List.of(
                "deploy with " + provider + " enabled but keep test banquet isolated",
                "confirm /api/admin/payments/providers reports productionReady=true",
                "create one low-value online gift payment order",
                "confirm prepayId and payPayload are stored on payment_order",
                "complete one real payment and verify callback SUCCESS",
                "replay one callback in non-production and verify duplicate handling is IGNORED",
                "monitor payment orders, callback logs, gift records, favor entries and broadcast logs"
        );
    }

    private List<String> rollbackSteps() {
        return List.of(
                "set PAYMENT_DEFAULT_PROVIDER=MOCK",
                "set PAYMENT_WECHAT_DIRECT_ENABLED=false if direct mode was enabled",
                "set PAYMENT_WECHAT_SP_ENABLED=false",
                "restart backend services",
                "run local acceptance smoke",
                "keep failed callback logs and payment orders for audit"
        );
    }

    private boolean callbackSecurityReady(PaymentProviderProperties.ProviderConfig config) {
        String mode = certificateMode(config);
        if ("PLATFORM_CERTIFICATE".equals(mode)) {
            return hasText(config.getPlatformCertificatePath());
        }
        if ("PUBLIC_KEY".equals(mode)) {
            return hasText(config.getWechatPayPublicKeyId()) && hasText(config.getWechatPayPublicKeyPath());
        }
        return config.hasApiV3Key();
    }

    private String callbackSecurityDetail(PaymentProviderProperties.ProviderConfig config) {
        String mode = certificateMode(config);
        if ("PLATFORM_CERTIFICATE".equals(mode)) {
            return hasText(config.getPlatformCertificatePath())
                    ? "platform certificate path configured"
                    : "missing platformCertificatePath";
        }
        if ("PUBLIC_KEY".equals(mode)) {
            return hasText(config.getWechatPayPublicKeyId()) && hasText(config.getWechatPayPublicKeyPath())
                    ? "WeChat Pay public key id/path configured"
                    : "missing wechatPayPublicKeyId or wechatPayPublicKeyPath";
        }
        return config.hasApiV3Key()
                ? "AUTO mode requires SDK certificate management and API v3 key"
                : "missing apiV3Key for AUTO mode";
    }

    private String certificateMode(PaymentProviderProperties.ProviderConfig config) {
        String mode = config.getCertificateMode();
        return mode == null || mode.isBlank() ? "AUTO" : mode.trim().toUpperCase();
    }

    private String callbackPath(PaymentProvider provider) {
        if (provider == PaymentProvider.WECHAT_DIRECT) {
            return "/api/payments/callbacks/wechat-direct";
        }
        if (provider == PaymentProvider.WECHAT_SERVICE_PROVIDER) {
            return "/api/payments/callbacks/wechat-service-provider";
        }
        return "/api/payments/callbacks";
    }

    private void addMissing(List<String> missing, String item, boolean configured) {
        if (!configured) {
            missing.add(item);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
