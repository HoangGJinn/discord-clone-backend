package com.discordclone.backend.service.voice;

import com.discordclone.backend.dto.voice.VoiceState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoiceStateServiceImpl implements VoiceStateService {

    // Đây là "Database trên RAM": Map<Kênh thoại ID, Map<User ID, Trạng thái Voice>>
    // Sử dụng ConcurrentHashMap để đảm bảo an toàn đa tiến trình (Thread-safe) khi nhiều người truy cập cùng lúc
    private final Map<Long, Map<String, VoiceState>> channelStates = new ConcurrentHashMap<>();

    // Map phụ trợ giúp tìm kiếm nhanh xem SessionID nào thuộc về User nào (Dùng lúc đứt kết nối)
    private final Map<String, VoiceState> sessionToStateMap = new ConcurrentHashMap<>();

    @Override
    public VoiceState joinChannel(String userId, Long channelId, Long serverId, String sessionId) {
        VoiceState oldState = getUserState(userId);

        // Nếu trước đó user đang ở một kênh voice khác (chưa rời mà đã vào kênh mới) -> Dọn dẹp cái cũ
        if (oldState != null && !oldState.getChannelId().equals(channelId)) {
            leaveChannel(userId, oldState.getChannelId());
        }

        // Tạo trạng thái mới cho user tại kênh hiện tại
        VoiceState state = VoiceState.builder()
                .userId(userId)
                .channelId(channelId)
                .serverId(serverId)
                .sessionId(sessionId)
                .isMuted(false)
                .isDeafened(false)
                .build();

        // Thêm vào Map lưu kênh
        channelStates.computeIfAbsent(channelId, k -> new ConcurrentHashMap<>()).put(userId, state);

        if (sessionId != null) {
            sessionToStateMap.put(sessionId, state);
        }

        return state;
    }

    @Override
    public VoiceState updateState (String userId, Long channelId, boolean isMuted, boolean isDeafened) {
        Map<String, VoiceState> usersInChannel = channelStates.get(channelId);

        if(usersInChannel != null && usersInChannel.containsKey(userId)) {

            VoiceState state = usersInChannel.get(userId);
            state.setMuted(isMuted);
            state.setDeafened(isDeafened);

            return state;
        }

        return null;
    }

    @Override
    public VoiceState leaveChannel (String userId, Long channelId) {
        Map<String, VoiceState> usersInChannel = channelStates.get(channelId);

        if(usersInChannel != null) {
            VoiceState removeState = usersInChannel.remove(userId);

            if (removeState != null && removeState.getSessionId() != null) {
                sessionToStateMap.remove(removeState.getSessionId());
            }

            if (usersInChannel.isEmpty()) {
               channelStates.remove(channelId);
            }

            return removeState;
        }

        return null;
    }

    @Override
    public List<VoiceState> getStatesByChannel (Long channelId) {
        Map<String, VoiceState> usersInChannel = channelStates.get(channelId);

        if (usersInChannel != null) {
            return new ArrayList<>(usersInChannel.values());
        }

        return new ArrayList<>();
    }

    @Override
    public VoiceState getAndRemoveUserBySessionId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        VoiceState state = sessionToStateMap.get(sessionId);

        if (state != null) {
            return leaveChannel(state.getUserId(), state.getChannelId());
        }

        return null;
    }

    @Override
    public VoiceState getUserState(String userId) {
        // Quét tất cả các kênh rà xem user này đang đứng ở đâu
        for (Map<String, VoiceState> channel : channelStates.values()) {
            if (channel.containsKey(userId)) {
                return channel.get(userId);
            }
        }

        return null;
    }
}
