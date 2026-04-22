package com.discordclone.backend.Controller.api.websocket;

import com.discordclone.backend.dto.voice.DMCallMessage;
import com.discordclone.backend.dto.voice.DMCallMessage.DMCallMessageType;
import com.discordclone.backend.dto.voice.DMCallState;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.service.voice.DMCallService;
import com.discordclone.backend.utils.agora.RtcTokenBuilder2;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DMCallWebSocketController {
    
    @Value("${agora.app-id}")
    private String appId;
    
    @Value("${agora.app-certificate}")
    private String appCertificate;
    
    private final int TOKEN_EXPIRE_TIME = 3600; // 1 giờ cho DM call
    
    private final DMCallService dmCallService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Người dùng gửi yêu cầu bắt đầu cuộc gọi
     * Client gửi: /app/dm/call/start
     */
    @MessageMapping("/dm/call/start")
    @SendTo("/topic/dm/call/{conversationId}")
    public DMCallMessage startCall(DMCallMessage message) {
        String conversationId = message.getConversationId();
        String callerId = message.getCallerId();
        String receiverId = message.getReceiverId();
        
        // Lấy thông tin user
        String callerName = "Unknown";
        String receiverName = "Unknown";
        String callerAvatar = null;
        String receiverAvatar = null;
        
        Optional<User> callerOpt = userRepository.findById(Long.parseLong(callerId));
        Optional<User> receiverOpt = userRepository.findById(Long.parseLong(receiverId));
        
        if (callerOpt.isPresent()) {
            User caller = callerOpt.get();
            callerName = caller.getDisplayName() != null ? caller.getDisplayName() : caller.getUserName();
            callerAvatar = caller.getAvatarUrl();
        }
        
        if (receiverOpt.isPresent()) {
            User receiver = receiverOpt.get();
            receiverName = receiver.getDisplayName() != null ? receiver.getDisplayName() : receiver.getUserName();
            receiverAvatar = receiver.getAvatarUrl();
        }
        
        // Xác định loại cuộc gọi
        DMCallState.CallType callType = DMCallState.CallType.VOICE;
        if (message.getCallType() != null) {
            try {
                callType = DMCallState.CallType.valueOf(message.getCallType().toUpperCase());
            } catch (Exception e) {
                // Default to VOICE if invalid
            }
        }
        
        // Tạo cuộc gọi mới
        DMCallState callState = dmCallService.initiateCall(
                conversationId, callerId, receiverId, 
                callerName, receiverName, 
                callerAvatar, receiverAvatar, 
                callType);
        
        // Tạo response
        DMCallMessage response = DMCallMessage.builder()
                .type(DMCallMessageType.CALL_INCOMING)
                .callState(callState)
                .conversationId(conversationId)
                .callerId(callerId)
                .receiverId(receiverId)
                .build();
        
        return response;
    }
    
    /**
     * Người nhận chấp nhận cuộc gọi
     * Client gửi: /app/dm/call/accept
     */
    @MessageMapping("/dm/call/accept")
    @SendTo("/topic/dm/call/{conversationId}")
    public DMCallMessage acceptCall(@DestinationVariable String conversationId, DMCallMessage message) {
        String userId = message.getReceiverId();
        
        DMCallState callState = dmCallService.acceptCall(conversationId, userId);
        
        if (callState != null) {
            // Tạo token cho cả hai người
            String callerToken = generateToken(conversationId, callState.getCallerId());
            String receiverToken = generateToken(conversationId, callState.getReceiverId());
            
            dmCallService.setTokens(conversationId, callerToken, receiverToken);
            
            return DMCallMessage.builder()
                    .type(DMCallMessageType.CALL_ACCEPTED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .build();
        }
        
        // Cuộc gọi không tồn tại hoặc không hợp lệ
        return DMCallMessage.builder()
                .type(DMCallMessageType.CALL_DECLINED)
                .conversationId(conversationId)
                .build();
    }
    
    /**
     * Người nhận từ chối cuộc gọi
     * Client gửi: /app/dm/call/decline
     */
    @MessageMapping("/dm/call/decline")
    @SendTo("/topic/dm/call/{conversationId}")
    public DMCallMessage declineCall(@DestinationVariable String conversationId, DMCallMessage message) {
        String userId = message.getReceiverId();
        
        DMCallState callState = dmCallService.declineCall(conversationId, userId);
        
        if (callState != null) {
            return DMCallMessage.builder()
                    .type(DMCallMessageType.CALL_DECLINED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .build();
        }
        
        return DMCallMessage.builder()
                .type(DMCallMessageType.CALL_DECLINED)
                .conversationId(conversationId)
                .build();
    }
    
    /**
     * Kết thúc cuộc gọi
     * Client gửi: /app/dm/call/end
     */
    @MessageMapping("/dm/call/end")
    @SendTo("/topic/dm/call/{conversationId}")
    public DMCallMessage endCall(@DestinationVariable String conversationId, DMCallMessage message) {
        String userId = message.getCallerId(); // Có thể là caller hoặc receiver
        
        DMCallState callState = dmCallService.endCall(conversationId, userId);
        
        if (callState != null) {
            return DMCallMessage.builder()
                    .type(DMCallMessageType.CALL_ENDED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .build();
        }
        
        return DMCallMessage.builder()
                .type(DMCallMessageType.CALL_ENDED)
                .conversationId(conversationId)
                .build();
    }
    
    /**
     * Cập nhật trạng thái mute/deafen
     * Client gửi: /app/dm/call/state
     */
    @MessageMapping("/dm/call/state")
    @SendTo("/topic/dm/call/{conversationId}")
    public DMCallMessage updateState(@DestinationVariable String conversationId, DMCallMessage message) {
        String userId = message.getCallerId();
        Boolean isMuted = message.getIsMuted();
        Boolean isDeafened = message.getIsDeafened();
        
        // Cập nhật với giá trị mặc định nếu không có
        boolean muteValue = isMuted != null ? isMuted : false;
        boolean deafenValue = isDeafened != null ? isDeafened : false;
        
        DMCallState callState = dmCallService.updateState(conversationId, userId, muteValue, deafenValue);
        
        if (callState != null) {
            return DMCallMessage.builder()
                    .type(DMCallMessageType.STATE_UPDATE)
                    .callState(callState)
                    .conversationId(conversationId)
                    .callerId(userId)
                    .isMuted(isMuted)
                    .isDeafened(isDeafened)
                    .build();
        }
        
        return null;
    }
    
    /**
     * Gửi token cho client (private message)
     * Client gửi: /app/dm/call/token/request
     */
    @MessageMapping("/dm/call/token/request")
    @SendToUser("/queue/call/token")
    public Map<String, Object> requestToken(DMCallMessage message) {
        String conversationId = message.getConversationId();
        String userId = message.getCallerId();
        
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra cuộc gọi có active không
        DMCallState callState = dmCallService.getCallState(conversationId);
        if (callState == null || callState.getStatus() != DMCallState.CallStatus.ACCEPTED) {
            response.put("error", "No active call");
            return response;
        }
        
        // Kiểm tra user có trong cuộc gọi không
        if (!callState.getCallerId().equals(userId) && !callState.getReceiverId().equals(userId)) {
            response.put("error", "User not in call");
            return response;
        }
        
        // Tạo token mới
        String token = generateToken(conversationId, userId);
        
        response.put("conversationId", conversationId);
        response.put("userId", userId);
        response.put("token", token);
        response.put("appId", appId);
        response.put("channelName", conversationId); // Agora channel = conversationId
        
        return response;
    }
    
    /**
     * Tạo Agora token
     */
    private String generateToken(String channelName, String userId) {
        if (appCertificate == null || appCertificate.isBlank()) {
            return "";
        }
        
        try {
            RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
            return tokenBuilder.buildTokenWithUserAccount(
                    appId,
                    appCertificate,
                    channelName,
                    userId,
                    RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                    TOKEN_EXPIRE_TIME,
                    TOKEN_EXPIRE_TIME);
        } catch (Exception e) {
            return "";
        }
    }
}