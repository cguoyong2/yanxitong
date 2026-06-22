package com.yanxitong.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHash {
    private static final String SHA256_PREFIX = "sha256:";

    private PasswordHash() {
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || !storedHash.startsWith(SHA256_PREFIX)) {
            return false;
        }
        return (SHA256_PREFIX + sha256Hex(rawPassword)).equalsIgnoreCase(storedHash);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
