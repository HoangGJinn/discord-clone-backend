package com.discordclone.backend.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DMCallMessage {
    private DMCallMessageType type;
    private DMCallState callState;
    
    // Các trường cho tin nhắn đơn lẻ (thay vì full state)
    private String conversationId;
    private String callerId;
    private String receiverId;
    private Boolean isMuted;
    private Boolean isDeafened;
    
    public enum DMCallMessageType {
        CALL_INCOMING,      // Có cuộc gọi đến
        CALL_ACCEPTED,      // Người kia nghe máy
        CALL_DECLINED,      // Người kia từ chối
        CALL_ENDED,         // Kết thúc cuộc gọi
        CALL_MISSED,        // Không ai nghe máy
        STATE_UPDATE,       // Cập nhật trạng thái mute/deafen
        TOKEN_REQUEST,      // Yêu cầu token Agora
        TOKEN_RESPONSE      // Trả token Agora
    }
}