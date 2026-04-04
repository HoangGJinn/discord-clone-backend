package com.discordclone.backend.config;

import com.discordclone.backend.dto.voice.VoiceMessage;
import com.discordclone.backend.dto.voice.VoiceState;
import com.discordclone.backend.entity.enums.VoiceMessageType;
import com.discordclone.backend.service.presence.PresenceService;
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
    private final PresenceService presenceService; // << Khai báo thêm service mới
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        // 1. Dọn dẹp Voice State (Nếu có)
        VoiceState disconnectedUserState = voiceStateService.getAndRemoveUserBySessionId(sessionId);
        if (disconnectedUserState != null) {
            log.info("User {} disconnected from voice channel {}",
                    disconnectedUserState.getUserId(), disconnectedUserState.getChannelId());
            VoiceMessage leaveMessage = VoiceMessage.builder()
                    .type(VoiceMessageType.LEAVE)
                    .state(disconnectedUserState)
                    .build();
            messagingTemplate.convertAndSend(
                    "/topic/server/" + disconnectedUserState.getServerId() + "/voice", leaveMessage);
        }

        // 2. Dọn dẹp Presence State (Tự động chuyển Offline nếu tắt hẳn app)
        Long offlineUserId = presenceService.userDisconnected(sessionId);
        if (offlineUserId != null) {
            log.info("User {} has completely gone Offline", offlineUserId);
            // Chú ý: Việc Broadcast đã được bao gồm bên trong code hàm `userDisconnected`
            // của PresenceServiceImpl (dùng messagingTemplate gửi đi rồi) nên không cần gửi lại ở đây.
        }
    }
}
