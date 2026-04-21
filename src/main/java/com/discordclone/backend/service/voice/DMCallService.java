package com.discordclone.backend.service.voice;

import com.discordclone.backend.dto.voice.DMCallState;
import com.discordclone.backend.dto.voice.DMCallState.CallStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DMCallService {
    
    // Lưu trạng thái cuộc gọi theo conversationId
    private final Map<String, DMCallState> activeCalls = new ConcurrentHashMap<>();
    
    // Lưu trạng thái cuộc gọi theo userId (để kiểm tra user có đang trong cuộc gọi không)
    private final Map<String, String> userToCallMap = new ConcurrentHashMap<>();
    
    /**
     * Tạo cuộc gọi mới
     */
    public DMCallState initiateCall(String conversationId, String callerId, String receiverId,
                                    String callerName, String receiverName,
                                    String callerAvatar, String receiverAvatar,
                                    DMCallState.CallType callType) {
        // Kiểm tra xem có cuộc gọi đang active không
        DMCallState existingCall = activeCalls.get(conversationId);
        if (existingCall != null && existingCall.getStatus() == CallStatus.PENDING) {
            // Đã có cuộc gọi đang chờ
            return existingCall;
        }
        
        // Tạo cuộc gọi mới
        String callId = UUID.randomUUID().toString();
        DMCallState callState = DMCallState.builder()
                .callId(callId)
                .conversationId(conversationId)
                .callerId(callerId)
                .receiverId(receiverId)
                .callerName(callerName)
                .receiverName(receiverName)
                .callerAvatar(callerAvatar)
                .receiverAvatar(receiverAvatar)
                .callType(callType != null ? callType : DMCallState.CallType.VOICE)
                .status(CallStatus.PENDING)
                .callerCameraOn(callType == DMCallState.CallType.VIDEO)
                .startedAt(System.currentTimeMillis())
                .build();
        
        activeCalls.put(conversationId, callState);
        userToCallMap.put(callerId, conversationId);
        userToCallMap.put(receiverId, conversationId);
        
        return callState;
    }
    
    /**
     * Người nhận chấp nhận cuộc gọi
     */
    public DMCallState acceptCall(String conversationId, String userId) {
        DMCallState callState = activeCalls.get(conversationId);
        if (callState == null) {
            return null;
        }
        
        // Chỉ người được gọi mới có thể chấp nhận
        if (!callState.getReceiverId().equals(userId)) {
            return null;
        }
        
        callState.setStatus(CallStatus.ACCEPTED);
        return callState;
    }
    
    /**
     * Người nhận từ chối cuộc gọi
     */
    public DMCallState declineCall(String conversationId, String userId) {
        DMCallState callState = activeCalls.get(conversationId);
        if (callState == null) {
            return null;
        }
        
        callState.setStatus(CallStatus.DECLINED);
        callState.setEndedAt(System.currentTimeMillis());
        
        // Dọn dẹp sau 30s
        scheduleCleanup(conversationId);
        
        return callState;
    }
    
    /**
     * Kết thúc cuộc gọi
     */
    public DMCallState endCall(String conversationId, String userId) {
        DMCallState callState = activeCalls.get(conversationId);
        if (callState == null) {
            return null;
        }
        
        // Kiểm tra user có trong cuộc gọi này không
        if (!callState.getCallerId().equals(userId) && !callState.getReceiverId().equals(userId)) {
            return null;
        }
        
        callState.setStatus(CallStatus.ENDED);
        callState.setEndedAt(System.currentTimeMillis());
        
        // Dọn dẹp sau 30s
        scheduleCleanup(conversationId);
        
        return callState;
    }
    
    /**
     * Cập nhật trạng thái mute/deafen
     */
    public DMCallState updateState(String conversationId, String userId, boolean isMuted, boolean isDeafened) {
        DMCallState callState = activeCalls.get(conversationId);
        if (callState == null || callState.getStatus() != CallStatus.ACCEPTED) {
            return null;
        }
        
        if (callState.getCallerId().equals(userId)) {
            callState.setCallerMuted(isMuted);
            callState.setCallerDeafened(isDeafened);
        } else if (callState.getReceiverId().equals(userId)) {
            callState.setReceiverMuted(isMuted);
            callState.setReceiverDeafened(isDeafened);
        } else {
            return null;
        }
        
        return callState;
    }
    
    /**
     * Lấy trạng thái cuộc gọi
     */
    public DMCallState getCallState(String conversationId) {
        return activeCalls.get(conversationId);
    }
    
    /**
     * Lấy cuộc gọi đang active của một user
     */
    public DMCallState getUserActiveCall(String userId) {
        String conversationId = userToCallMap.get(userId);
        if (conversationId != null) {
            return activeCalls.get(conversationId);
        }
        return null;
    }
    
    /**
     * Kiểm tra user có đang trong cuộc gọi không
     */
    public boolean isUserInCall(String userId) {
        return userToCallMap.containsKey(userId);
    }
    
    /**
     * Kiểm tra user có đang trong cuộc gọi cụ thể không
     */
    public boolean isUserInCall(String userId, String conversationId) {
        String activeCallId = userToCallMap.get(userId);
        return conversationId.equals(activeCallId);
    }
    
    /**
     * Lưu token Agora cho cuộc gọi
     */
    public void setTokens(String conversationId, String callerToken, String receiverToken) {
        DMCallState callState = activeCalls.get(conversationId);
        if (callState != null) {
            if (callerToken != null) {
                callState.setCallerToken(callerToken);
            }
            if (receiverToken != null) {
                callState.setReceiverToken(receiverToken);
            }
        }
    }
    
    /**
     * Lấy token của user trong cuộc gọi
     */
    public String getUserToken(String conversationId, String userId) {
        DMCallState callState = activeCalls.get(conversationId);
        if (callState == null) {
            return null;
        }
        
        if (callState.getCallerId().equals(userId)) {
            return callState.getCallerToken();
        } else if (callState.getReceiverId().equals(userId)) {
            return callState.getReceiverToken();
        }
        return null;
    }
    
    /**
     * Xóa cuộc gọi khỏi bộ nhớ
     */
    public void removeCall(String conversationId) {
        DMCallState callState = activeCalls.remove(conversationId);
        if (callState != null) {
            userToCallMap.remove(callState.getCallerId());
            userToCallMap.remove(callState.getReceiverId());
        }
    }
    
    /**
     * Hẹn giờ dọn dẹp cuộc gọi
     */
    private void scheduleCleanup(String conversationId) {
        new Thread(() -> {
            try {
                Thread.sleep(30000); // 30 giây
                removeCall(conversationId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}