package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class WechatPayClientFactoryTests {
    @Test
    void validateEnabledRejectsDisabledProvider() {
        PaymentProviderProperties properties = properties(config -> config.setEnabled(false));
        WechatPayClientFactory factory = new WechatPayClientFactory(properties);

        assertThrows(UnsupportedOperationException.class, () -> factory.validateEnabled(factoryConfig(properties)));
    }

    @Test
    void validateRequiredRejectsMissingCoreFields() {
        PaymentProviderProperties properties = properties(config -> {
            config.setEnabled(true);
            config.setMerchantId("1900000000");
        });
        WechatPayClientFactory factory = new WechatPayClientFactory(properties);

        assertThrows(IllegalArgumentException.class, () -> factory.validateRequired(factoryConfig(properties)));
    }

    @Test
    void validateRequiredAcceptsAutoCertificateFields() {
        PaymentProviderProperties.ProviderConfig config = completeConfig();
        config.setCertificateMode("AUTO");
        WechatPayClientFactory factory = new WechatPayClientFactory(properties(config));

        assertDoesNotThrow(() -> factory.validateRequired(config));
    }

    @Test
    void validateRequiredRejectsPublicKeyModeWithoutPublicKeyFields() {
        PaymentProviderProperties.ProviderConfig config = completeConfig();
        config.setCertificateMode("PUBLIC_KEY");
        WechatPayClientFactory factory = new WechatPayClientFactory(properties(config));

        assertThrows(IllegalArgumentException.class, () -> factory.validateRequired(config));
    }

    private PaymentProviderProperties.ProviderConfig factoryConfig(PaymentProviderProperties properties) {
        return properties.provider(PaymentProvider.WECHAT_SERVICE_PROVIDER);
    }

    private PaymentProviderProperties properties(java.util.function.Consumer<PaymentProviderProperties.ProviderConfig> customizer) {
        PaymentProviderProperties.ProviderConfig config = new PaymentProviderProperties.ProviderConfig();
        customizer.accept(config);
        return properties(config);
    }

    private PaymentProviderProperties properties(PaymentProviderProperties.ProviderConfig config) {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.setProviders(Map.of(PaymentProvider.WECHAT_SERVICE_PROVIDER, config));
        return properties;
    }

    private PaymentProviderProperties.ProviderConfig completeConfig() {
        PaymentProviderProperties.ProviderConfig config = new PaymentProviderProperties.ProviderConfig();
        config.setEnabled(true);
        config.setMerchantId("1900000000");
        config.setAppId("wx-app");
        config.setServiceProviderId("1900000000");
        config.setCertificateSerialNo("SERIAL");
        config.setPrivateKeyPath("/tmp/apiclient_key.pem");
        config.setApiV3Key("12345678901234567890123456789012");
        config.setNotifyUrl("https://example.com/api/payments/callbacks/wechat-service-provider");
        return config;
    }
}
