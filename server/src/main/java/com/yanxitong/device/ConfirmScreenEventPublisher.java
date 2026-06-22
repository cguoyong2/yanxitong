package com.yanxitong.device;

import com.yanxitong.device.dto.ConfirmScreenGiftEvent;
import com.yanxitong.device.websocket.ConfirmScreenWebSocketHandler;
import org.springframework.stereotype.Service;

@Service
public class ConfirmScreenEventPublisher {
    private final ConfirmScreenWebSocketHandler webSocketHandler;

    public ConfirmScreenEventPublisher(ConfirmScreenWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public int publishGiftPaid(ConfirmScreenGiftEvent event) {
        return webSocketHandler.sendToBanquet(event.banquetId(), event);
    }
}
