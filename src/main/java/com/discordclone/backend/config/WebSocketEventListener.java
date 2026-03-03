package com.discordclone.backend.config;

import com.discordclone.backend.dto.voice.VoiceMessage;
import com.discordclone.backend.dto.voice.VoiceState;
import com.discordclone.backend.entity.enums.VoiceMessageType;
import com.discordclone.backend.service.voice.VoiceStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final VoiceStateService voiceStateService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        // Kiểm tra xem Session bị rớt này có đang bật Voice Channel không
        VoiceState disconnectedUserState = voiceStateService.getAndRemoveUserBySessionId(sessionId);

        if (disconnectedUserState != null) {
            log.info("User {} disconnected from voice channel {}",
                    disconnectedUserState.getUserId(), disconnectedUserState.getChannelId());
            // Tự động đóng gói một tin nhắn LEAVE
            VoiceMessage leaveMessage = VoiceMessage.builder()
                    .type(VoiceMessageType.LEAVE)
                    .state(disconnectedUserState)
                    .build();
            // Phát Broadcast báo cho các người dùng khác trong server biết người này đã rớt mạng (rời phòng)
            messagingTemplate.convertAndSend(
                    "/topic/server/" + disconnectedUserState.getServerId() + "/voice", leaveMessage);
        }
    }
}
