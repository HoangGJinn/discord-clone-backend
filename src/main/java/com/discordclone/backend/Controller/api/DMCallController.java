package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.voice.DMCallMessage;
import com.discordclone.backend.dto.voice.DMCallState;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.entity.mongo.Conversation;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.repository.mongo.ConversationRepository;
import com.discordclone.backend.service.voice.DMCallService;
import com.discordclone.backend.utils.agora.RtcTokenBuilder2;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dm/call")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class DMCallController {
    
    @Value("${agora.app-id}")
    private String appId;
    
    @Value("${agora.app-certificate}")
    private String appCertificate;
    
    private final int TOKEN_EXPIRE_TIME = 3600; // 1 giờ
    
    private final DMCallService dmCallService;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Lấy token Agora cho DM call
     * GET /api/dm/call/token?conversationId=xxx&userId=xxx
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken(
            @RequestParam String conversationId,
            @RequestParam String userId) {
        
        try {
            // Kiểm tra cuộc gọi có active không
            DMCallState callState = dmCallService.getCallState(conversationId);
            if (callState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không có cuộc gọi nào đang active");
            }
            
            // Kiểm tra user có trong cuộc gọi không
            if (!callState.getCallerId().equals(userId) && !callState.getReceiverId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Bạn không tham gia cuộc gọi này");
            }
            
            // Kiểm tra cuộc gọi đã được chấp nhận chưa
            if (callState.getStatus() != DMCallState.CallStatus.ACCEPTED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cuộc gọi chưa được chấp nhận");
            }
            
            // Tạo token
            String token = generateToken(conversationId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("appId", appId);
            response.put("channelName", conversationId);
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi tạo token: " + e.getMessage());
        }
    }
    
    /**
     * Bắt đầu cuộc gọi
     * POST /api/dm/call/start
     */
    @PostMapping("/start")
    public ResponseEntity<?> startCall(@RequestBody Map<String, String> body) {
        try {
            String conversationId = body.get("conversationId");
            String callerId = body.get("callerId");
            
            if (conversationId == null || callerId == null) {
                return ResponseEntity.badRequest().body("Thiếu thông tin");
            }
            
            // Lấy thông tin conversation
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (convOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation không tồn tại");
            }
            
            Conversation conversation = convOpt.get();
            
            // Xác định receiver
            String receiverId;
            if (conversation.getUser1Id().toString().equals(callerId)) {
                receiverId = conversation.getUser2Id().toString();
            } else {
                receiverId = conversation.getUser1Id().toString();
            }
            
            // Lấy tên user
            String callerName = "Unknown";
            String receiverName = "Unknown";
            
            Optional<User> callerOpt = userRepository.findById(Long.parseLong(callerId));
            if (callerOpt.isPresent()) {
                User caller = callerOpt.get();
                callerName = caller.getDisplayName() != null ? caller.getDisplayName() : caller.getUserName();
            }
            
            Optional<User> receiverOpt = userRepository.findById(Long.parseLong(receiverId));
            if (receiverOpt.isPresent()) {
                User receiver = receiverOpt.get();
                receiverName = receiver.getDisplayName() != null ? receiver.getDisplayName() : receiver.getUserName();
            }
            
            // Tạo cuộc gọi
            DMCallState callState = dmCallService.initiateCall(
                    conversationId, callerId, receiverId, callerName, receiverName);
            
            // Broadcast qua WebSocket để thông báo cho người nhận
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.CALL_INCOMING)
                    .callState(callState)
                    .conversationId(conversationId)
                    .callerId(callerId)
                    .receiverId(receiverId)
                    .build();
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            
            return ResponseEntity.ok(callState);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi bắt đầu cuộc gọi: " + e.getMessage());
        }
    }
    
    /**
     * Chấp nhận cuộc gọi
     * POST /api/dm/call/accept
     */
    @PostMapping("/accept")
    public ResponseEntity<?> acceptCall(@RequestBody Map<String, String> body) {
        try {
            String conversationId = body.get("conversationId");
            String userId = body.get("userId");
            
            if (conversationId == null || userId == null) {
                return ResponseEntity.badRequest().body("Thiếu thông tin");
            }
            
            DMCallState callState = dmCallService.acceptCall(conversationId, userId);
            if (callState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy cuộc gọi hoặc không có quyền");
            }
            
            // Tạo tokens cho cả hai
            String callerToken = generateToken(conversationId, callState.getCallerId());
            String receiverToken = generateToken(conversationId, callState.getReceiverId());
            dmCallService.setTokens(conversationId, callerToken, receiverToken);
            
            // Broadcast qua WebSocket
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.CALL_ACCEPTED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .build();
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            
            return ResponseEntity.ok(callState);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi chấp nhận cuộc gọi: " + e.getMessage());
        }
    }
    
    /**
     * Từ chối cuộc gọi
     * POST /api/dm/call/decline
     */
    @PostMapping("/decline")
    public ResponseEntity<?> declineCall(@RequestBody Map<String, String> body) {
        try {
            String conversationId = body.get("conversationId");
            String userId = body.get("userId");
            
            DMCallState callState = dmCallService.declineCall(conversationId, userId);
            if (callState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy cuộc gọi");
            }
            
            // Broadcast qua WebSocket
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.CALL_DECLINED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .build();
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            
            return ResponseEntity.ok(callState);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * Kết thúc cuộc gọi
     * POST /api/dm/call/end
     */
    @PostMapping("/end")
    public ResponseEntity<?> endCall(@RequestBody Map<String, String> body) {
        try {
            String conversationId = body.get("conversationId");
            String userId = body.get("userId");
            
            DMCallState callState = dmCallService.endCall(conversationId, userId);
            if (callState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy cuộc gọi");
            }
            
            // Broadcast qua WebSocket
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.CALL_ENDED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .build();
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            
            return ResponseEntity.ok(callState);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * Cập nhật trạng thái mute/deafen
     * POST /api/dm/call/state
     */
    @PostMapping("/state")
    public ResponseEntity<?> updateState(@RequestBody Map<String, Object> body) {
        try {
            String conversationId = (String) body.get("conversationId");
            String userId = (String) body.get("userId");
            Boolean isMuted = (Boolean) body.get("isMuted");
            Boolean isDeafened = (Boolean) body.get("isDeafened");
            
            boolean muteValue = isMuted != null ? isMuted : false;
            boolean deafenValue = isDeafened != null ? isDeafened : false;
            
            DMCallState callState = dmCallService.updateState(conversationId, userId, muteValue, deafenValue);
            if (callState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy cuộc gọi");
            }
            
            // Broadcast state update qua WebSocket
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.STATE_UPDATE)
                    .callState(callState)
                    .conversationId(conversationId)
                    .isMuted(muteValue)
                    .isDeafened(deafenValue)
                    .build();
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            
            return ResponseEntity.ok(callState);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * Lấy trạng thái cuộc gọi
     * GET /api/dm/call/status?conversationId=xxx
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam String conversationId) {
        DMCallState callState = dmCallService.getCallState(conversationId);
        if (callState == null) {
            return ResponseEntity.ok(Map.of("hasActiveCall", false));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasActiveCall", true);
        response.put("callState", callState);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Tạo Agora token
     */
    private String generateToken(String channelName, String userId) {
        if (appCertificate == null || appCertificate.isBlank()) {
            return ""; // Testing mode
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