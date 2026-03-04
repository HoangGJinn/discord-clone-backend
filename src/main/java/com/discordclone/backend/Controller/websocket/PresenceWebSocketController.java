package com.discordclone.backend.Controller.websocket;

import com.discordclone.backend.dto.presence.PresenceMessage;
import com.discordclone.backend.entity.enums.UserStatus;
import com.discordclone.backend.service.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PresenceWebSocketController {
    private final PresenceService presenceService;
    // Client gửi lệnh lên /app/presence/connect khi mỏ app/login thành công
    @MessageMapping("/presence/connect")
    public void handleUserConnect(@Payload PresenceMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Long userId = message.getUserId();
        if (userId != null) {
            // Đăng ký người dùng này vào hệ thống Presence
            presenceService.userConnected(userId, sessionId);
        }
    }

    // Client gửi lệnh /app/presence/update khi người dùng cố tình chuyển sang mode DND hoặc IDLE
    @MessageMapping("/presence/update")
    public void handleStatusUpdate(@Payload PresenceMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = message.getUserId();
        UserStatus newStatus = message.getStatus();

        if (userId != null && newStatus != null) {
            presenceService.updateUserStatus(userId, newStatus);
        }
    }
}
