package com.yanxitong.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yanxitong.payment.controller.AdminPaymentController;
import com.yanxitong.payment.dto.PaymentLaunchReadiness;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminPaymentControllerTests {
    @Test
    void launchReadinessGroupsMissingProviderConfiguration() {
        AdminPaymentController controller = new AdminPaymentController(
                mock(PaymentCallbackService.class),
                new PaymentProviderProperties()
        );

        PaymentLaunchReadiness readiness = controller.launchReadiness(PaymentProvider.WECHAT_SERVICE_PROVIDER).data();

        assertFalse(readiness.ready());
        assertEquals(3, readiness.groups().size());
        assertTrue(readiness.blockers().contains("支付通道已启用"));
        assertTrue(readiness.groups().stream().anyMatch(group ->
                "provider-config".equals(group.code()) && !group.ready() && group.blockers().contains("核心商户配置完整")));
    }

    @Test
    void launchReadinessPassesWhenProviderConfigurationIsComplete() {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        PaymentProviderProperties.ProviderConfig config = new PaymentProviderProperties.ProviderConfig();
        config.setEnabled(true);
        config.setMerchantId("sp-merchant");
        config.setAppId("sp-app");
        config.setServiceProviderId("sp-merchant");
        config.setSubMerchantId("sub-merchant");
        config.setCertificateSerialNo("cert-serial");
        config.setPrivateKeyPath("/run/secrets/wechat/apiclient_key.pem");
        config.setApiV3Key("api-v3-key");
        config.setNotifyUrl("https://pay.example.com/api/payments/callbacks/wechat-service-provider");
        properties.setDefaultProvider(PaymentProvider.WECHAT_SERVICE_PROVIDER);
        properties.setProviders(Map.of(PaymentProvider.WECHAT_SERVICE_PROVIDER, config));
        AdminPaymentController controller = new AdminPaymentController(
                mock(PaymentCallbackService.class),
                properties
        );

        PaymentLaunchReadiness readiness = controller.launchReadiness(PaymentProvider.WECHAT_SERVICE_PROVIDER).data();

        assertTrue(readiness.ready());
        assertTrue(readiness.blockers().isEmpty());
        assertTrue(readiness.groups().stream().allMatch(PaymentLaunchReadiness.ReadinessGroup::ready));
    }
}
