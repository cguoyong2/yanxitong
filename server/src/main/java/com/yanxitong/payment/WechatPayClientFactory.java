package com.yanxitong.payment;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.AutoCertificateNotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RSANotificationConfig;
import com.wechat.pay.java.core.notification.RSAPublicKeyNotificationConfig;
import org.springframework.stereotype.Component;

@Component
public class WechatPayClientFactory {
    private final PaymentProviderProperties properties;

    public WechatPayClientFactory(PaymentProviderProperties properties) {
        this.properties = properties;
    }

    public PreparedWechatPayClient prepare(PaymentProvider provider) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider);
        validateEnabled(config);
        validateRequired(provider, config);
        String mode = certificateMode(config);
        Config sdkConfig = buildConfig(config, mode);
        NotificationParser notificationParser = buildNotificationParser(config, mode);
        return new PreparedWechatPayClient(sdkConfig, notificationParser, mode);
    }

    public void validateEnabled(PaymentProviderProperties.ProviderConfig config) {
        if (!config.isEnabled()) {
            throw new UnsupportedOperationException("Wechat service provider is not enabled");
        }
    }

    public void validateRequired(PaymentProviderProperties.ProviderConfig config) {
        validateRequired(PaymentProvider.WECHAT_SERVICE_PROVIDER, config);
    }

    public void validateRequired(PaymentProvider provider, PaymentProviderProperties.ProviderConfig config) {
        require(config.getMerchantId(), "Wechat merchant-id is required");
        require(config.getAppId(), "Wechat app-id is required");
        if (provider == PaymentProvider.WECHAT_SERVICE_PROVIDER) {
            require(config.getServiceProviderId(), "Wechat service-provider-id is required");
            require(config.getSubMerchantId(), "Wechat sub-merchant-id is required");
        }
        require(config.getCertificateSerialNo(), "Wechat certificate-serial-no is required");
        require(config.getPrivateKeyPath(), "Wechat private-key-path is required");
        require(config.getApiV3Key(), "Wechat api-v3-key is required");
        require(config.getNotifyUrl(), "Wechat notify-url is required");
        String mode = certificateMode(config);
        if ("PLATFORM_CERTIFICATE".equals(mode)) {
            require(config.getPlatformCertificatePath(), "Wechat platform-certificate-path is required in PLATFORM_CERTIFICATE mode");
        }
        if ("PUBLIC_KEY".equals(mode)) {
            require(config.getWechatPayPublicKeyId(), "Wechat public-key-id is required in PUBLIC_KEY mode");
            require(config.getWechatPayPublicKeyPath(), "Wechat public-key-path is required in PUBLIC_KEY mode");
        }
    }

    private Config buildConfig(PaymentProviderProperties.ProviderConfig config, String mode) {
        if ("PUBLIC_KEY".equals(mode)) {
            return new RSAPublicKeyConfig.Builder()
                    .merchantId(config.getMerchantId())
                    .privateKeyFromPath(config.getPrivateKeyPath())
                    .merchantSerialNumber(config.getCertificateSerialNo())
                    .apiV3Key(config.getApiV3Key())
                    .publicKeyId(config.getWechatPayPublicKeyId())
                    .publicKeyFromPath(config.getWechatPayPublicKeyPath())
                    .build();
        }
        if ("PLATFORM_CERTIFICATE".equals(mode)) {
            return new RSAConfig.Builder()
                    .merchantId(config.getMerchantId())
                    .privateKeyFromPath(config.getPrivateKeyPath())
                    .merchantSerialNumber(config.getCertificateSerialNo())
                    .wechatPayCertificatesFromPath(config.getPlatformCertificatePath())
                    .build();
        }
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(config.getMerchantId())
                .privateKeyFromPath(config.getPrivateKeyPath())
                .merchantSerialNumber(config.getCertificateSerialNo())
                .apiV3Key(config.getApiV3Key())
                .build();
    }

    private NotificationParser buildNotificationParser(PaymentProviderProperties.ProviderConfig config, String mode) {
        if ("PUBLIC_KEY".equals(mode)) {
            return new NotificationParser(new RSAPublicKeyNotificationConfig.Builder()
                    .publicKeyId(config.getWechatPayPublicKeyId())
                    .publicKeyFromPath(config.getWechatPayPublicKeyPath())
                    .apiV3Key(config.getApiV3Key())
                    .build());
        }
        if ("PLATFORM_CERTIFICATE".equals(mode)) {
            return new NotificationParser(new RSANotificationConfig.Builder()
                    .certificatesFromPath(config.getPlatformCertificatePath())
                    .apiV3Key(config.getApiV3Key())
                    .build());
        }
        return new NotificationParser(new AutoCertificateNotificationConfig.Builder()
                .merchantId(config.getMerchantId())
                .privateKeyFromPath(config.getPrivateKeyPath())
                .merchantSerialNumber(config.getCertificateSerialNo())
                .apiV3Key(config.getApiV3Key())
                .build());
    }

    private String certificateMode(PaymentProviderProperties.ProviderConfig config) {
        String mode = config.getCertificateMode();
        return mode == null || mode.isBlank() ? "AUTO" : mode.trim().toUpperCase();
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record PreparedWechatPayClient(
            Config config,
            NotificationParser notificationParser,
            String certificateMode
    ) {
    }
}
