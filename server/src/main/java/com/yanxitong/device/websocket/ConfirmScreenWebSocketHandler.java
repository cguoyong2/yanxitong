package com.yanxitong.device.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ConfirmScreenWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessionsByBanquet = new ConcurrentHashMap<>();

    public ConfirmScreenWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long banquetId = banquetId(session.getUri());
        if (banquetId != null) {
            session.getAttributes().put("banquetId", banquetId);
            sessionsByBanquet.computeIfAbsent(banquetId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("PING".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object banquetId = session.getAttributes().get("banquetId");
        if (banquetId instanceof Long id) {
            Set<WebSocketSession> sessions = sessionsByBanquet.get(id);
            if (sessions != null) {
                sessions.remove(session);
            }
        }
    }

    public int sendToBanquet(Long banquetId, Object event) {
        Set<WebSocketSession> sessions = sessionsByBanquet.get(banquetId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize confirm-screen event", e);
        }
        sessions.removeIf(session -> !session.isOpen());
        sessions.forEach(session -> send(session, payload));
        return sessions.size();
    }

    public int onlineSessions(Long banquetId) {
        Set<WebSocketSession> sessions = sessionsByBanquet.get(banquetId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        sessions.removeIf(session -> !session.isOpen());
        return sessions.size();
    }

    private void send(WebSocketSession session, String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException ignored) {
            // Closed sessions are cleaned on the next send.
        }
    }

    private Long banquetId(URI uri) {
        if (uri == null) {
            return null;
        }
        String value = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("banquetId");
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }
}
