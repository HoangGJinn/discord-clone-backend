package com.discordclone.backend.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceState {
    private String userId;      // Lưu id user
    private Long channelId;     // Kênh thoại mà user đang ở
    private Long serverId;      // Server chứa kênh thoại đó
    private String sessionId;   // ID của session WebSocket (để xử lý lúc mất mạng)

    @Builder.Default
    private boolean isMuted = false;    // Trạng thái tắt mic

    @Builder.Default
    private boolean isDeafened = false; // Trạng thái tắt âm
}
