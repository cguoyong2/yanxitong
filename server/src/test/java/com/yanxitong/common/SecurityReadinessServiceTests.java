package com.yanxitong.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.auth.entity.AdminUser;
import com.yanxitong.auth.mapper.AdminUserMapper;
import com.yanxitong.payment.PaymentProvider;
import com.yanxitong.payment.PaymentProviderProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityReadinessServiceTests {
    @Test
    void localDefaultsReturnWarningsAndMockSuccessBlocker() {
        SecurityReadinessService service = new SecurityReadinessService(
                environment("local"),
                paymentProperties(true, PaymentProvider.MOCK, "yanxitong-mock-callback-secret"),
                adminMapper(defaultAdmin())
        );

        SecurityReadinessResult result = service.check();

        assertEquals("BLOCKED", result.status());
        assertFalse(result.productionReady());
        assertTrue(result.blockers().contains("Mock 支付成功入口已关闭"));
        assertTrue(result.warnings().contains("默认管理员密码已更换"));
        assertTrue(result.warnings().contains("数据库密码不是本地默认值"));
    }

    @Test
    void productionDefaultsBecomeBlockers() {
        SecurityReadinessService service = new SecurityReadinessService(
                environment("production"),
                paymentProperties(false, PaymentProvider.MOCK, "yanxitong-mock-callback-secret"),
                adminMapper(defaultAdmin())
        );

        SecurityReadinessResult result = service.check();

        assertEquals("BLOCKED", result.status());
        assertTrue(result.blockers().contains("默认管理员密码已更换"));
        assertTrue(result.blockers().contains("数据库密码不是本地默认值"));
        assertTrue(result.blockers().contains("默认支付通道不是 MOCK"));
        assertTrue(result.blockers().contains("默认支付通道生产配置完整"));
    }

    @Test
    void hardenedProductionConfigurationIsReady() throws Exception {
        SecurityReadinessService service = new SecurityReadinessService(
                environment("production")
                        .withProperty("spring.datasource.password", "strong-db-password")
                        .withProperty("spring.data.redis.password", "strong-redis-password"),
                productionPaymentProperties(),
                adminMapper(null)
        );

        SecurityReadinessResult result = service.check();

        assertEquals("READY", result.status());
        assertTrue(result.productionReady());
        assertTrue(result.blockers().isEmpty());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void directWechatProductionConfigurationDoesNotRequireServiceProviderFields() throws Exception {
        SecurityReadinessService service = new SecurityReadinessService(
                environment("production")
                        .withProperty("spring.datasource.password", "strong-db-password")
                        .withProperty("spring.data.redis.password", "strong-redis-password"),
                directWechatPaymentProperties(),
                adminMapper(null)
        );

        SecurityReadinessResult result = service.check();

        assertEquals("READY", result.status());
        assertTrue(result.productionReady());
        assertTrue(result.blockers().isEmpty());
    }

    @Test
    void invalidWechatPrivateKeyBlocksProductionReadiness() throws Exception {
        Path invalidKey = Files.createTempFile("wechat-invalid-key", ".pem");
        Files.writeString(invalidKey, "not-a-private-key", StandardCharsets.US_ASCII);
        PaymentProviderProperties properties = paymentProperties(false, PaymentProvider.WECHAT_DIRECT, "strong-mock-secret");
        PaymentProviderProperties.ProviderConfig wechatConfig = new PaymentProviderProperties.ProviderConfig();
        wechatConfig.setEnabled(true);
        wechatConfig.setMerchantId("direct-merchant");
        wechatConfig.setAppId("direct-app");
        wechatConfig.setCertificateSerialNo("cert-serial");
        wechatConfig.setPrivateKeyPath(invalidKey.toString());
        wechatConfig.setApiV3Key("api-v3-key");
        wechatConfig.setNotifyUrl("https://pay.example.com/api/payments/callbacks/wechat-direct");
        properties.setProviders(Map.of(
                PaymentProvider.MOCK, properties.provider(PaymentProvider.MOCK),
                PaymentProvider.WECHAT_DIRECT, wechatConfig
        ));
        SecurityReadinessService service = new SecurityReadinessService(
                environment("production")
                        .withProperty("spring.datasource.password", "strong-db-password")
                        .withProperty("spring.data.redis.password", "strong-redis-password"),
                properties,
                adminMapper(null)
        );

        SecurityReadinessResult result = service.check();

        assertEquals("BLOCKED", result.status());
        assertTrue(result.blockers().contains("默认支付通道生产配置完整"));
    }

    private MockEnvironment environment(String appEnv) {
        return new MockEnvironment()
                .withProperty("APP_ENV", appEnv)
                .withProperty("spring.datasource.password", "yanxitong")
                .withProperty("spring.data.redis.password", "");
    }

    private PaymentProviderProperties paymentProperties(boolean mockSuccessEnabled, PaymentProvider defaultProvider, String mockSecret) {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        PaymentProviderProperties.ProviderConfig mockConfig = new PaymentProviderProperties.ProviderConfig();
        mockConfig.setEnabled(true);
        mockConfig.setCallbackSecret(mockSecret);
        properties.setDefaultProvider(defaultProvider);
        properties.setMockSuccessEnabled(mockSuccessEnabled);
        properties.setProviders(Map.of(PaymentProvider.MOCK, mockConfig));
        return properties;
    }

    private PaymentProviderProperties productionPaymentProperties() throws Exception {
        PaymentProviderProperties properties = paymentProperties(false, PaymentProvider.WECHAT_SERVICE_PROVIDER, "strong-mock-secret");
        PaymentProviderProperties.ProviderConfig wechatConfig = new PaymentProviderProperties.ProviderConfig();
        wechatConfig.setEnabled(true);
        wechatConfig.setMerchantId("sp-merchant");
        wechatConfig.setAppId("sp-app");
        wechatConfig.setServiceProviderId("sp-merchant");
        wechatConfig.setSubMerchantId("sub-merchant");
        wechatConfig.setCertificateSerialNo("cert-serial");
        wechatConfig.setPrivateKeyPath(writeTestPrivateKey());
        wechatConfig.setApiV3Key("api-v3-key");
        wechatConfig.setNotifyUrl("https://pay.example.com/api/payments/callbacks/wechat-service-provider");
        properties.setProviders(Map.of(
                PaymentProvider.MOCK, properties.provider(PaymentProvider.MOCK),
                PaymentProvider.WECHAT_SERVICE_PROVIDER, wechatConfig
        ));
        return properties;
    }

    private PaymentProviderProperties directWechatPaymentProperties() throws Exception {
        PaymentProviderProperties properties = paymentProperties(false, PaymentProvider.WECHAT_DIRECT, "strong-mock-secret");
        PaymentProviderProperties.ProviderConfig wechatConfig = new PaymentProviderProperties.ProviderConfig();
        wechatConfig.setEnabled(true);
        wechatConfig.setMerchantId("direct-merchant");
        wechatConfig.setAppId("direct-app");
        wechatConfig.setCertificateSerialNo("cert-serial");
        wechatConfig.setPrivateKeyPath(writeTestPrivateKey());
        wechatConfig.setApiV3Key("api-v3-key");
        wechatConfig.setNotifyUrl("https://pay.example.com/api/payments/callbacks/wechat-direct");
        properties.setProviders(Map.of(
                PaymentProvider.MOCK, properties.provider(PaymentProvider.MOCK),
                PaymentProvider.WECHAT_DIRECT, wechatConfig
        ));
        return properties;
    }

    private AdminUserMapper adminMapper(AdminUser user) {
        AdminUserMapper mapper = mock(AdminUserMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(user);
        return mapper;
    }

    private AdminUser defaultAdmin() {
        AdminUser user = new AdminUser();
        user.username = "admin";
        user.status = "ACTIVE";
        user.passwordHash = "sha256:240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
        return user;
    }

    private String writeTestPrivateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        byte[] encoded = generator.generateKeyPair().getPrivate().getEncoded();
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(encoded);
        Path path = Files.createTempFile("wechat-test-key", ".pem");
        Files.writeString(path, "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n", StandardCharsets.US_ASCII);
        return path.toString();
    }
}
