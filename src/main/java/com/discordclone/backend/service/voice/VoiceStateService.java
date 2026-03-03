package com.discordclone.backend.service.voice;

import com.discordclone.backend.dto.voice.VoiceState;

import java.util.List;

public interface VoiceStateService {
    // 1. Thêm một người dùng vào kênh thoại
    VoiceState joinChannel(String userId, Long channelId, Long serverId, String sessionId);

    // 2. Cập nhật lại trạng thái (Tắt Mic/Tắt âm) của người dùng
    VoiceState updateState(String userId, Long channelId, boolean isMuted, boolean isDeafened);

    // 3. Xóa một người dùng khỏi kênh thoại một cách chủ động (nhấn nút Leave)
    VoiceState leaveChannel(String userId, Long channelId);

    // 4. Lấy danh sách toàn bộ những người dùng đang ở trong 1 kênh cụ thể
    List<VoiceState> getStatesByChannel(Long channelId);

    // 5. Tìm xem một session WebSocket (khi bị đứt kết nối mạng) thuộc về user nào, ở phòng nào
    VoiceState getAndRemoveUserBySessionId(String sessionId);

    // 6. Tìm xem hiện tại 1 user đang kết nối ở kênh voice nào
    VoiceState getUserState(String userId);
}
