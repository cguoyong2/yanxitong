package com.yanxitong.payment;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProviderProperties {
    private PaymentProvider defaultProvider = PaymentProvider.MOCK;
    private boolean mockSuccessEnabled;
    private Map<PaymentProvider, ProviderConfig> providers = new EnumMap<>(PaymentProvider.class);

    public PaymentProvider getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(PaymentProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public boolean isMockSuccessEnabled() {
        return mockSuccessEnabled;
    }

    public void setMockSuccessEnabled(boolean mockSuccessEnabled) {
        this.mockSuccessEnabled = mockSuccessEnabled;
    }

    public Map<PaymentProvider, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<PaymentProvider, ProviderConfig> providers) {
        this.providers = providers == null ? new EnumMap<>(PaymentProvider.class) : providers;
    }

    public ProviderConfig provider(PaymentProvider provider) {
        return providers.getOrDefault(provider, new ProviderConfig());
    }

    public static class ProviderConfig {
        private boolean enabled;
        private String merchantId;
        private String appId;
        private String serviceProviderId;
        private String subMerchantId;
        private String subAppId;
        private String callbackSecret;
        private String certificateSerialNo;
        private String privateKeyPath;
        private String apiV3Key;
        private String notifyUrl;
        private String certificateMode = "AUTO";
        private String platformCertificatePath;
        private String wechatPayPublicKeyId;
        private String wechatPayPublicKeyPath;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(String merchantId) {
            this.merchantId = merchantId;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getServiceProviderId() {
            return serviceProviderId;
        }

        public void setServiceProviderId(String serviceProviderId) {
            this.serviceProviderId = serviceProviderId;
        }

        public String getSubMerchantId() {
            return subMerchantId;
        }

        public void setSubMerchantId(String subMerchantId) {
            this.subMerchantId = subMerchantId;
        }

        public String getSubAppId() {
            return subAppId;
        }

        public void setSubAppId(String subAppId) {
            this.subAppId = subAppId;
        }

        public String getCallbackSecret() {
            return callbackSecret;
        }

        public void setCallbackSecret(String callbackSecret) {
            this.callbackSecret = callbackSecret;
        }

        public String getCertificateSerialNo() {
            return certificateSerialNo;
        }

        public void setCertificateSerialNo(String certificateSerialNo) {
            this.certificateSerialNo = certificateSerialNo;
        }

        public boolean hasCallbackSecret() {
            return callbackSecret != null && !callbackSecret.isBlank();
        }

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public String getApiV3Key() {
            return apiV3Key;
        }

        public void setApiV3Key(String apiV3Key) {
            this.apiV3Key = apiV3Key;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public void setNotifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
        }

        public String getCertificateMode() {
            return certificateMode;
        }

        public void setCertificateMode(String certificateMode) {
            this.certificateMode = certificateMode;
        }

        public String getPlatformCertificatePath() {
            return platformCertificatePath;
        }

        public void setPlatformCertificatePath(String platformCertificatePath) {
            this.platformCertificatePath = platformCertificatePath;
        }

        public String getWechatPayPublicKeyId() {
            return wechatPayPublicKeyId;
        }

        public void setWechatPayPublicKeyId(String wechatPayPublicKeyId) {
            this.wechatPayPublicKeyId = wechatPayPublicKeyId;
        }

        public String getWechatPayPublicKeyPath() {
            return wechatPayPublicKeyPath;
        }

        public void setWechatPayPublicKeyPath(String wechatPayPublicKeyPath) {
            this.wechatPayPublicKeyPath = wechatPayPublicKeyPath;
        }

        public boolean hasApiV3Key() {
            return apiV3Key != null && !apiV3Key.isBlank();
        }

        public boolean hasPrivateKeyPath() {
            return privateKeyPath != null && !privateKeyPath.isBlank();
        }

        public boolean hasNotifyUrl() {
            return notifyUrl != null && !notifyUrl.isBlank();
        }

        public boolean hasWechatPayPublicKey() {
            return wechatPayPublicKeyPath != null && !wechatPayPublicKeyPath.isBlank()
                    && wechatPayPublicKeyId != null && !wechatPayPublicKeyId.isBlank();
        }
    }
}
