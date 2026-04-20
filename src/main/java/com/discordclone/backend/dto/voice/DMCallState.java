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
    private String callerAvatar;
    private String receiverAvatar;
    
    @Builder.Default
    private CallType callType = CallType.VOICE;
    
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
    
    @Builder.Default
    private boolean callerCameraOn = false;
    
    @Builder.Default
    private boolean receiverCameraOn = false;
    
    public enum CallStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        ENDED,
        MISSED
    }
    
    public enum CallType {
        VOICE,
        VIDEO
    }
}