package com.discordclone.backend.Controller.api.websocket;

import com.discordclone.backend.dto.voice.VoiceMessage;
import com.discordclone.backend.dto.voice.VoiceState;
import com.discordclone.backend.entity.enums.VoiceMessageType;
import com.discordclone.backend.service.voice.VoiceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class VoiceWebSocketController {
    private final VoiceStateService voiceStateService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/voice/action")
    public void handleVoiceAction (@Payload VoiceMessage message, SimpMessageHeaderAccessor headerAccessor) {

        // Lấy session ID của người gửi (để xử lý rớt mạng sau này)
        String sessionId = headerAccessor.getSessionId();
        VoiceState state = message.getState();

        if (state == null || state.getUserId() == null || state.getChannelId() == null) {
            return; // Dữ liệu không hợp lệ
        }

        switch (message.getType()) {
            case JOIN:
                VoiceState newState = voiceStateService.joinChannel(
                        state.getUserId(), state.getChannelId(), state.getServerId(), sessionId
                );

                List<VoiceState> currentStates = voiceStateService.getStatesByChannel(state.getChannelId());
                VoiceMessage syncMessage = VoiceMessage.builder()
                        .type(VoiceMessageType.INITIAL_SYNC)
                        .states(currentStates)
                        .build();

                // Gửi riêng cho User đó qua destination user (Yêu cầu config WebSocket hỗ trợ User Destination)
                // Hoặc đơn giản là gửi lại chính xác cho session đó (tùy vào cách cấu hình bảo mật dự án)
                // Ở đây ta đơn giản gửi chung kênh để mọi người cùng cập nhật luôn
                messagingTemplate.convertAndSend(
                        "/topic/server/" + state.getServerId() + "/voice", syncMessage);
                break;

            case LEAVE:
                // 1. Xóa user khỏi phòng
                voiceStateService.leaveChannel(state.getUserId(), state.getChannelId());

                // 2. Báo lại cho toàn server biết là user này vừa out
                messagingTemplate.convertAndSend(
                        "/topic/server/" + state.getServerId() + "/voice", message);
                break;

            case UPDATE_STATE:
                // 1. Update trạng thái (Mute/Deafen) trên RAM server
                VoiceState updatedState = voiceStateService.updateState(
                        state.getUserId(), state.getChannelId(), state.isMuted(), state.isDeafened());

                // 2. Phát Broadcast trạng thái mới để các người dùng khác hiện thị UI "Mic icon bị gạch chéo"
                if (updatedState != null) {
                    message.setState(updatedState); // Cập nhật lại payload chuẩn trước khi bung ra
                    messagingTemplate.convertAndSend(
                            "/topic/server/" + state.getServerId() + "/voice", message);
                }
                break;

            default:
                break;
        }
    }
}
