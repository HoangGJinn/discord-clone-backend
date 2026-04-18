package com.discordclone.backend.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DMCallState {
    private String callId;
    private String conversationId;
    private String callerId;
    private String receiverId;
    private String callerName;
    private String receiverName;
    
    @Builder.Default
    private CallStatus status = CallStatus.PENDING;
    
    private String callerToken;
    private String receiverToken;
    
    private long startedAt;
    private long endedAt;
    
    @Builder.Default
    private boolean callerMuted = false;
    
    @Builder.Default
    private boolean receiverMuted = false;
    
    @Builder.Default
    private boolean callerDeafened = false;
    
    @Builder.Default
    private boolean receiverDeafened = false;
    
    public enum CallStatus {
        PENDING,    // Đang đổ chuông
        ACCEPTED,  // Người kia nghe máy
        DECLINED,   // Người kia từ chối
        ENDED,      // Kết thúc cuộc gọi
        MISSED      // Không ai nghe máy
    }
}