package com.yanxitong.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.auth.entity.AdminUser;
import com.yanxitong.auth.mapper.AdminUserMapper;
import com.yanxitong.payment.PaymentProvider;
import com.yanxitong.payment.PaymentPrivateKeyValidator;
import com.yanxitong.payment.PaymentProviderProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SecurityReadinessService {
    private static final String DEFAULT_ADMIN_HASH = "sha256:240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
    private static final String DEFAULT_DB_PASSWORD = "yanxitong";
    private static final String DEFAULT_MOCK_CALLBACK_SECRET = "yanxitong-mock-callback-secret";

    private final Environment environment;
    private final PaymentProviderProperties paymentProperties;
    private final AdminUserMapper adminUserMapper;

    public SecurityReadinessService(
            Environment environment,
            PaymentProviderProperties paymentProperties,
            AdminUserMapper adminUserMapper
    ) {
        this.environment = environment;
        this.paymentProperties = paymentProperties;
        this.adminUserMapper = adminUserMapper;
    }

    public SecurityReadinessResult check() {
        String appEnv = environment.getProperty("APP_ENV", "local");
        List<String> activeProfiles = Arrays.stream(environment.getActiveProfiles()).toList();
        boolean productionMode = isProduction(appEnv, activeProfiles);
        List<SecurityReadinessResult.Item> items = new ArrayList<>();

        items.add(item(
                "app-environment",
                "运行环境已显式标识",
                hasText(environment.getProperty("APP_ENV")) || !activeProfiles.isEmpty(),
                productionMode ? "BLOCKER" : "WARNING",
                "APP_ENV=" + appEnv + ", profiles=" + activeProfiles
        ));
        items.add(item(
                "admin-default-password",
                "默认管理员密码已更换",
                !defaultAdminPasswordPresent(),
                productionMode ? "BLOCKER" : "WARNING",
                "default admin account admin/admin123 must not remain active in production"
        ));
        items.add(item(
                "database-password",
                "数据库密码不是本地默认值",
                !DEFAULT_DB_PASSWORD.equals(environment.getProperty("spring.datasource.password")),
                productionMode ? "BLOCKER" : "WARNING",
                "spring.datasource.password must be provided from production secrets"
        ));
        items.add(item(
                "redis-password",
                "Redis 密码已配置",
                hasText(environment.getProperty("spring.data.redis.password")),
                productionMode ? "BLOCKER" : "WARNING",
                "spring.data.redis.password is blank"
        ));
        items.add(item(
                "mock-callback-secret",
                "Mock 回调密钥不是默认值",
                !DEFAULT_MOCK_CALLBACK_SECRET.equals(paymentProperties.provider(PaymentProvider.MOCK).getCallbackSecret()),
                productionMode ? "BLOCKER" : "WARNING",
                "PAYMENT_MOCK_CALLBACK_SECRET must not use local default"
        ));
        items.add(item(
                "mock-success-disabled",
                "Mock 支付成功入口已关闭",
                !paymentProperties.isMockSuccessEnabled(),
                "BLOCKER",
                "PAYMENT_MOCK_SUCCESS_ENABLED must stay false outside local acceptance"
        ));
        items.add(item(
                "payment-provider",
                "默认支付通道不是 MOCK",
                paymentProperties.getDefaultProvider() != PaymentProvider.MOCK,
                productionMode ? "BLOCKER" : "WARNING",
                "PAYMENT_DEFAULT_PROVIDER=" + paymentProperties.getDefaultProvider()
        ));
        items.add(item(
                "payment-provider-enabled",
                "默认支付通道已启用",
                defaultPaymentProviderEnabled(),
                productionMode ? "BLOCKER" : "WARNING",
                "default provider enabled=" + defaultPaymentProviderEnabled()
        ));
        items.add(item(
                "payment-provider-production-config",
                "默认支付通道生产配置完整",
                defaultPaymentProviderProductionConfigReady(),
                productionMode ? "BLOCKER" : "WARNING",
                defaultPaymentProviderConfigDetail()
        ));

        List<String> blockers = items.stream()
                .filter(item -> !item.passed() && "BLOCKER".equals(item.severity()))
                .map(SecurityReadinessResult.Item::label)
                .toList();
        List<String> warnings = items.stream()
                .filter(item -> !item.passed() && !"BLOCKER".equals(item.severity()))
                .map(SecurityReadinessResult.Item::label)
                .toList();
        String status = blockers.isEmpty() && warnings.isEmpty() ? "READY" : blockers.isEmpty() ? "WARN" : "BLOCKED";
        return new SecurityReadinessResult(status, blockers.isEmpty() && warnings.isEmpty(), appEnv, activeProfiles, blockers, warnings, items);
    }

    private SecurityReadinessResult.Item item(String code, String label, boolean passed, String severity, String detail) {
        return new SecurityReadinessResult.Item(code, label, passed, severity, detail);
    }

    private boolean defaultAdminPasswordPresent() {
        AdminUser user = adminUserMapper.selectOne(new QueryWrapper<AdminUser>()
                .eq("username", "admin")
                .eq("status", "ACTIVE")
                .last("LIMIT 1"));
        return user != null && DEFAULT_ADMIN_HASH.equalsIgnoreCase(user.passwordHash);
    }

    private boolean isProduction(String appEnv, List<String> activeProfiles) {
        if ("production".equalsIgnoreCase(appEnv) || "prod".equalsIgnoreCase(appEnv)) {
            return true;
        }
        return activeProfiles.stream().anyMatch(profile -> "production".equalsIgnoreCase(profile) || "prod".equalsIgnoreCase(profile));
    }

    private boolean defaultPaymentProviderEnabled() {
        PaymentProviderProperties.ProviderConfig config = paymentProperties.provider(paymentProperties.getDefaultProvider());
        return config.isEnabled();
    }

    private boolean defaultPaymentProviderProductionConfigReady() {
        PaymentProvider provider = paymentProperties.getDefaultProvider();
        if (provider == PaymentProvider.MOCK) {
            return false;
        }
        return missingPaymentProviderItems(provider, paymentProperties.provider(provider)).isEmpty();
    }

    private String defaultPaymentProviderConfigDetail() {
        PaymentProvider provider = paymentProperties.getDefaultProvider();
        List<String> missing = missingPaymentProviderItems(provider, paymentProperties.provider(provider));
        if (provider == PaymentProvider.MOCK) {
            return "MOCK cannot be production default provider";
        }
        return missing.isEmpty()
                ? provider + " provider config is complete"
                : provider + " missing: " + String.join(", ", missing);
    }

    private List<String> missingPaymentProviderItems(PaymentProvider provider, PaymentProviderProperties.ProviderConfig config) {
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
        if (config.hasPrivateKeyPath() && !PaymentPrivateKeyValidator.validate(config.getPrivateKeyPath()).valid()) {
            missing.add("privateKeyReadable");
        }
        addMissing(missing, "apiV3Key", config.hasApiV3Key());
        addMissing(missing, "notifyUrl", config.hasNotifyUrl());
        String mode = config.getCertificateMode() == null || config.getCertificateMode().isBlank()
                ? "AUTO"
                : config.getCertificateMode().trim().toUpperCase();
        if ("PLATFORM_CERTIFICATE".equals(mode)) {
            addMissing(missing, "platformCertificatePath", hasText(config.getPlatformCertificatePath()));
        }
        if ("PUBLIC_KEY".equals(mode)) {
            addMissing(missing, "wechatPayPublicKeyId", hasText(config.getWechatPayPublicKeyId()));
            addMissing(missing, "wechatPayPublicKeyPath", hasText(config.getWechatPayPublicKeyPath()));
        }
        return missing;
    }

    private void addMissing(List<String> missing, String item, boolean configured) {
        if (!configured) {
            missing.add(item);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
