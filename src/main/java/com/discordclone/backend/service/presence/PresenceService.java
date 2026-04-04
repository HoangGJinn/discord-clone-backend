package com.discordclone.backend.service.presence;

import com.discordclone.backend.entity.enums.UserStatus;

public interface PresenceService {
    // 1. Khi có người dùng mở ứng dụng và kết nối WebSocket
    void userConnected(Long  userId, String sessionId);

    // 2. Khi người dùng đóng ứng dụng hoặc rớt mạng (trả về User ID nếu họ thực sự Offline)
    Long userDisconnected(String sessionId);

    // 3. Khi người dùng muốn ĐỔI TRẠNG THÁI BẰNG TAY (vd: bấm chuyển sang DND)
    void updateUserStatus(Long  userId, UserStatus newStatus);

    // 4. Lấy trạng thái hiện tại của 1 người
    UserStatus getUserStatus(Long  userId);
}
