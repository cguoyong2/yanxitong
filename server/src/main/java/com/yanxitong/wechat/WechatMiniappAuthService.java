package com.yanxitong.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class WechatMiniappAuthService {
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WechatMiniappProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WechatMiniappAuthService(WechatMiniappProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String resolveOpenId(String code) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Wechat miniapp app id or app secret is not configured");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Wechat login code is required");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CODE2SESSION_URL
                        + "?appid=" + encode(properties.getAppId())
                        + "&secret=" + encode(properties.getAppSecret())
                        + "&js_code=" + encode(code)
                        + "&grant_type=authorization_code"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            String openId = body.path("openid").asText("");
            if (openId.isBlank()) {
                String errorCode = body.path("errcode").asText("");
                String errorMessage = body.path("errmsg").asText("Wechat code2session failed");
                throw new IllegalStateException(errorCode.isBlank() ? errorMessage : errorCode + ": " + errorMessage);
            }
            return openId;
        } catch (IOException ex) {
            throw new IllegalStateException("Wechat code2session response parse failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Wechat code2session request interrupted", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
