package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yanxitong.payment.controller.AdminPaymentController;
import com.yanxitong.payment.dto.PaymentLaunchReadiness;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminPaymentControllerTests {
    @Test
    void launchReadinessGroupsMissingProviderConfiguration() {
        AdminPaymentController controller = new AdminPaymentController(
                mock(PaymentCallbackService.class),
                new PaymentProviderReadinessService(new PaymentProviderProperties())
        );

        PaymentLaunchReadiness readiness = controller.launchReadiness(PaymentProvider.WECHAT_SERVICE_PROVIDER).data();

        assertFalse(readiness.ready());
        assertEquals(3, readiness.groups().size());
        assertTrue(readiness.blockers().contains("支付通道已启用"));
        assertTrue(readiness.groups().stream().anyMatch(group ->
                "provider-config".equals(group.code()) && !group.ready() && group.blockers().contains("核心商户配置完整")));
    }

    @Test
    void launchReadinessPassesWhenProviderConfigurationIsComplete() throws Exception {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        PaymentProviderProperties.ProviderConfig config = new PaymentProviderProperties.ProviderConfig();
        config.setEnabled(true);
        config.setMerchantId("sp-merchant");
        config.setAppId("sp-app");
        config.setServiceProviderId("sp-merchant");
        config.setSubMerchantId("sub-merchant");
        config.setCertificateSerialNo("cert-serial");
        config.setPrivateKeyPath(writeTestPrivateKey());
        config.setApiV3Key("api-v3-key");
        config.setNotifyUrl("https://pay.example.com/api/payments/callbacks/wechat-service-provider");
        properties.setDefaultProvider(PaymentProvider.WECHAT_SERVICE_PROVIDER);
        properties.setProviders(Map.of(PaymentProvider.WECHAT_SERVICE_PROVIDER, config));
        AdminPaymentController controller = new AdminPaymentController(
                mock(PaymentCallbackService.class),
                new PaymentProviderReadinessService(properties)
        );

        PaymentLaunchReadiness readiness = controller.launchReadiness(PaymentProvider.WECHAT_SERVICE_PROVIDER).data();

        assertTrue(readiness.ready());
        assertTrue(readiness.blockers().isEmpty());
        assertTrue(readiness.groups().stream().allMatch(PaymentLaunchReadiness.ReadinessGroup::ready));
    }

    @Test
    void launchReadinessPassesForDirectMerchantWithoutSubMerchantConfiguration() throws Exception {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        PaymentProviderProperties.ProviderConfig config = new PaymentProviderProperties.ProviderConfig();
        config.setEnabled(true);
        config.setMerchantId("direct-merchant");
        config.setAppId("wx-direct-app");
        config.setCertificateSerialNo("cert-serial");
        config.setPrivateKeyPath(writeTestPrivateKey());
        config.setApiV3Key("api-v3-key");
        config.setNotifyUrl("https://pay.example.com/api/payments/callbacks/wechat-direct");
        properties.setDefaultProvider(PaymentProvider.WECHAT_DIRECT);
        properties.setProviders(Map.of(PaymentProvider.WECHAT_DIRECT, config));
        AdminPaymentController controller = new AdminPaymentController(
                mock(PaymentCallbackService.class),
                new PaymentProviderReadinessService(properties)
        );

        PaymentLaunchReadiness readiness = controller.launchReadiness(PaymentProvider.WECHAT_DIRECT).data();

        assertTrue(readiness.ready());
        assertTrue(readiness.blockers().isEmpty());
        assertTrue(readiness.groups().stream().allMatch(PaymentLaunchReadiness.ReadinessGroup::ready));
    }

    @Test
    void launchReadinessBlocksInvalidPrivateKeyFile() throws Exception {
        Path invalidKey = Files.createTempFile("wechat-invalid-key", ".pem");
        Files.writeString(invalidKey, "not-a-private-key", StandardCharsets.US_ASCII);
        PaymentProviderProperties properties = new PaymentProviderProperties();
        PaymentProviderProperties.ProviderConfig config = new PaymentProviderProperties.ProviderConfig();
        config.setEnabled(true);
        config.setMerchantId("direct-merchant");
        config.setAppId("wx-direct-app");
        config.setCertificateSerialNo("cert-serial");
        config.setPrivateKeyPath(invalidKey.toString());
        config.setApiV3Key("api-v3-key");
        config.setNotifyUrl("https://pay.example.com/api/payments/callbacks/wechat-direct");
        properties.setDefaultProvider(PaymentProvider.WECHAT_DIRECT);
        properties.setProviders(Map.of(PaymentProvider.WECHAT_DIRECT, config));
        AdminPaymentController controller = new AdminPaymentController(
                mock(PaymentCallbackService.class),
                new PaymentProviderReadinessService(properties)
        );

        PaymentLaunchReadiness readiness = controller.launchReadiness(PaymentProvider.WECHAT_DIRECT).data();

        assertFalse(readiness.ready());
        assertTrue(readiness.blockers().contains("私钥文件可读取并可解析"));
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
