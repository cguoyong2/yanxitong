package com.yanxitong.payment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public final class PaymentPrivateKeyValidator {
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    private PaymentPrivateKeyValidator() {
    }

    public static ValidationResult validate(String privateKeyPath) {
        if (privateKeyPath == null || privateKeyPath.isBlank()) {
            return new ValidationResult(false, "未配置微信支付 API 私钥文件路径");
        }
        Path path = Path.of(privateKeyPath);
        if (!Files.isRegularFile(path)) {
            return new ValidationResult(false, "微信支付 API 私钥文件不存在");
        }
        if (!Files.isReadable(path)) {
            return new ValidationResult(false, "微信支付 API 私钥文件不可读取");
        }
        try {
            String pem = Files.readString(path, StandardCharsets.US_ASCII).replace("\r", "");
            int begin = pem.indexOf(BEGIN_PRIVATE_KEY);
            int end = pem.indexOf(END_PRIVATE_KEY);
            if (begin < 0 || end < 0 || end <= begin) {
                return new ValidationResult(false, "微信支付 API 私钥文件格式不正确，请使用 apiclient_key.pem");
            }
            String base64 = pem.substring(begin + BEGIN_PRIVATE_KEY.length(), end).replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getMimeDecoder().decode(base64);
            KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            return new ValidationResult(true, "微信支付 API 私钥文件可读取并可解析");
        } catch (Exception ex) {
            return new ValidationResult(false, "微信支付 API 私钥文件不可解析，请重新上传正确的 apiclient_key.pem");
        }
    }

    public record ValidationResult(boolean valid, String detail) {
    }
}
