package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.voice.DMCallMessage;
import com.discordclone.backend.dto.voice.DMCallState;
import com.discordclone.backend.dto.response.DirectMessageResponse;
import com.discordclone.backend.dto.response.UserResponse;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.entity.mongo.Conversation;
import com.discordclone.backend.entity.mongo.DirectMessage;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.repository.mongo.ConversationRepository;
import com.discordclone.backend.repository.mongo.DirectMessageRepository;
import com.discordclone.backend.service.voice.DMCallService;
import com.discordclone.backend.utils.agora.RtcTokenBuilder2;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    private final DirectMessageRepository directMessageRepository;
    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private void sendSocketToUser(Long userId, Object payload) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getUserName() != null) {
                messagingTemplate.convertAndSendToUser(user.getUserName(), "/queue/dm", payload);
            }
        });
    }
    
    /**
     * Lấy token Agora cho DM call
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken(
            @RequestParam String conversationId,
            @RequestParam String userId) {
        
        try {
            DMCallState callState = dmCallService.getCallState(conversationId);
            if (callState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không có cuộc gọi nào đang active");
            }
            
            if (!callState.getCallerId().equals(userId) && !callState.getReceiverId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không tham gia cuộc gọi này");
            }
            
            if (callState.getStatus() != DMCallState.CallStatus.ACCEPTED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cuộc gọi chưa được chấp nhận");
            }
            
            String token = generateToken(conversationId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("appId", appId);
            response.put("channelName", conversationId);
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi tạo token: " + e.getMessage());
        }
    }
    
    @PostMapping("/start")
    public ResponseEntity<?> startCall(@RequestBody Map<String, String> body) {
        try {
            String conversationId = body.get("conversationId");
            String callerId = body.get("callerId");
            String callTypeStr = body.getOrDefault("callType", "VOICE");
            
            if (conversationId == null || callerId == null) {
                return ResponseEntity.badRequest().body("Thiếu thông tin");
            }
            
            DMCallState.CallType callType;
            try {
                callType = DMCallState.CallType.valueOf(callTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                callType = DMCallState.CallType.VOICE;
            }
            
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (convOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation không tồn tại");
            
            Conversation conversation = convOpt.get();
            String receiverId = conversation.getUser1Id().toString().equals(callerId) 
                ? conversation.getUser2Id().toString() 
                : conversation.getUser1Id().toString();
            
            String callerName = "Unknown", receiverName = "Unknown", callerAvatar = null, receiverAvatar = null;
            
            Optional<User> callerOpt = userRepository.findById(Long.parseLong(callerId));
            if (callerOpt.isPresent()) {
                User caller = callerOpt.get();
                callerName = caller.getDisplayName() != null ? caller.getDisplayName() : caller.getUserName();
                callerAvatar = caller.getAvatarUrl();
            }
            
            Optional<User> receiverOpt = userRepository.findById(Long.parseLong(receiverId));
            if (receiverOpt.isPresent()) {
                User receiver = receiverOpt.get();
                receiverName = receiver.getDisplayName() != null ? receiver.getDisplayName() : receiver.getUserName();
                receiverAvatar = receiver.getAvatarUrl();
            }
            
            DMCallState callState = dmCallService.initiateCall(conversationId, callerId, receiverId, 
                callerName, receiverName, callerAvatar, receiverAvatar, callType);
            
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.CALL_INCOMING)
                    .callState(callState)
                    .conversationId(conversationId)
                    .callerId(callerId)
                    .receiverId(receiverId)
                    .callerName(callerName)
                    .callerAvatar(callerAvatar)
                    .callType(callType.name())
                    .build();
            
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            messagingTemplate.convertAndSend("/topic/user/" + receiverId + "/incoming-call", wsMessage);
            
            return ResponseEntity.ok(callState);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi bắt đầu cuộc gọi: " + e.getMessage());
        }
    }
    
    @PostMapping("/accept")
    public ResponseEntity<?> acceptCall(@RequestBody Map<String, String> body) {
        try {
            String conversationId = body.get("conversationId"), userId = body.get("userId");
            DMCallState callState = dmCallService.acceptCall(conversationId, userId);
            if (callState == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy cuộc gọi");
            
            if (callState.getCallType() == DMCallState.CallType.VIDEO) callState.setReceiverCameraOn(true);
            
            String callerToken = generateToken(conversationId, callState.getCallerId());
            String receiverToken = generateToken(conversationId, callState.getReceiverId());
            dmCallService.setTokens(conversationId, callerToken, receiverToken);
            
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.CALL_ACCEPTED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .callType(callState.getCallType().name())
                    .build();
            
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            messagingTemplate.convertAndSend("/topic/user/" + callState.getCallerId() + "/incoming-call", wsMessage);
            messagingTemplate.convertAndSend("/topic/user/" + callState.getReceiverId() + "/incoming-call", wsMessage);
            
            return ResponseEntity.ok(callState);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
    
    @PostMapping("/decline")
    public ResponseEntity<?> declineCall(@RequestBody Map<String, String> body) {
        try {
            String conversationId = body.get("conversationId"), userId = body.get("userId");
            DMCallState callState = dmCallService.declineCall(conversationId, userId);
            if (callState == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy cuộc gọi");
            
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.CALL_DECLINED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .callType(callState.getCallType().name())
                    .build();
            
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            messagingTemplate.convertAndSend("/topic/user/" + callState.getCallerId() + "/incoming-call", wsMessage);
            messagingTemplate.convertAndSend("/topic/user/" + callState.getReceiverId() + "/incoming-call", wsMessage);
            
            saveCallMessage(conversationId, Long.parseLong(userId), "Declined a " + callState.getCallType().name().toLowerCase() + " call");
            
            return ResponseEntity.ok(callState);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
    
    @PostMapping("/end")
    public ResponseEntity<?> endCall(@RequestBody Map<String, String> body) {
        try {
            String conversationId = body.get("conversationId"), userId = body.get("userId");
            DMCallState callState = dmCallService.endCall(conversationId, userId);
            if (callState == null) {
                return ResponseEntity.ok(Map.of(
                        "conversationId", conversationId,
                        "status", "ENDED"
                ));
            }
            
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.CALL_ENDED)
                    .callState(callState)
                    .conversationId(conversationId)
                    .callType(callState.getCallType().name())
                    .build();
            
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            messagingTemplate.convertAndSend("/topic/user/" + callState.getCallerId() + "/incoming-call", wsMessage);
            messagingTemplate.convertAndSend("/topic/user/" + callState.getReceiverId() + "/incoming-call", wsMessage);
            
            long duration = (System.currentTimeMillis() - callState.getStartedAt()) / 1000;
            saveCallMessage(conversationId, Long.parseLong(userId), "Call ended • " + String.format("%d:%02d", duration / 60, duration % 60));
            
            return ResponseEntity.ok(callState);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
    
    @PostMapping("/state")
    public ResponseEntity<?> updateState(@RequestBody Map<String, Object> body) {
        try {
            String conversationId = (String) body.get("conversationId"), userId = (String) body.get("userId");
            Boolean isMuted = (Boolean) body.get("isMuted"), isDeafened = (Boolean) body.get("isDeafened"), isCameraOn = (Boolean) body.get("isCameraOn");
            
            DMCallState callState = dmCallService.updateState(conversationId, userId, isMuted != null ? isMuted : false, isDeafened != null ? isDeafened : false);
            if (callState == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy cuộc gọi");
            
            if (isCameraOn != null) {
                if (callState.getCallerId().equals(userId)) callState.setCallerCameraOn(isCameraOn);
                else if (callState.getReceiverId().equals(userId)) callState.setReceiverCameraOn(isCameraOn);
            }
            
            DMCallMessage wsMessage = DMCallMessage.builder()
                    .type(DMCallMessage.DMCallMessageType.STATE_UPDATE)
                    .callState(callState)
                    .conversationId(conversationId)
                    .isMuted(isMuted).isDeafened(isDeafened).isCameraOn(isCameraOn)
                    .callType(callState.getCallType().name())
                    .build();
            messagingTemplate.convertAndSend("/topic/dm/call/" + conversationId, wsMessage);
            
            return ResponseEntity.ok(callState);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam String conversationId) {
        DMCallState callState = dmCallService.getCallState(conversationId);
        if (callState == null) return ResponseEntity.ok(Map.of("hasActiveCall", false));
        return ResponseEntity.ok(Map.of("hasActiveCall", true, "callState", callState));
    }
    
    private String generateToken(String channelName, String userId) {
        if (appCertificate == null || appCertificate.isBlank()) return "";
        try {
            return new RtcTokenBuilder2().buildTokenWithUserAccount(appId, appCertificate, channelName, userId, 
                RtcTokenBuilder2.Role.ROLE_PUBLISHER, TOKEN_EXPIRE_TIME, TOKEN_EXPIRE_TIME);
        } catch (Exception e) { return ""; }
    }
    
    private void saveCallMessage(String conversationId, Long senderId, String content) {
        try {
            Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
            if (conversationOpt.isEmpty()) {
                return;
            }

            Conversation conversation = conversationOpt.get();
            Long receiverId = Objects.equals(conversation.getUser1Id(), senderId)
                    ? conversation.getUser2Id()
                    : conversation.getUser1Id();

            DirectMessage message = DirectMessage.builder()
                    .conversationId(conversationId)
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .content("[Call] " + content)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .isRead(false)
                    .build();

            DirectMessage saved = directMessageRepository.save(message);
            DirectMessageResponse response = mapToResponse(saved);
            messagingTemplate.convertAndSend("/topic/dm/" + conversationId, response);
            sendSocketToUser(receiverId, response);
            sendSocketToUser(senderId, response);
        } catch (Exception e) { System.err.println("Lỗi lưu call message: " + e.getMessage()); }
    }

    private DirectMessageResponse mapToResponse(DirectMessage dm) {
        User sender = null;
        User receiver = null;

        if (dm.getSenderId() != null) {
            sender = userRepository.findById(dm.getSenderId()).orElse(null);
        }
        if (dm.getReceiverId() != null) {
            receiver = userRepository.findById(dm.getReceiverId()).orElse(null);
        }
        return DirectMessageResponse.builder()
                .id(dm.getId())
                .conversationId(dm.getConversationId())
                .senderId(dm.getSenderId())
                .receiverId(dm.getReceiverId())
                .sender(UserResponse.from(sender))
                .receiver(UserResponse.from(receiver))
                .content(dm.getContent())
                .createdAt(dm.getCreatedAt())
                .updatedAt(dm.getUpdatedAt())
                .edited(dm.isEdited())
                .deleted(dm.isDeleted())
                .isRead(dm.isRead())
                .attachments(dm.getAttachments())
                .replyToId(dm.getReplyToId())
                .replyToMessage(null)
                .reactions(dm.getReactions())
                .build();
    }
}