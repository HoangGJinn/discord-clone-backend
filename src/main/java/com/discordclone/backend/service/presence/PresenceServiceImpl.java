package com.discordclone.backend.service.presence;

import com.discordclone.backend.dto.presence.PresenceMessage;
import com.discordclone.backend.entity.enums.UserStatus;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService{

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Map số 1: Theo dõi xem 1 User đang đăng nhập ở BAO NHIÊU thiết bị (Mobile, Web...)
    // Ví dụ: User "A" -> [Session1 (Web), Session2 (Mobile)]
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    // Map số 2: Từ 1 Session tìm ngược ra ngay được User ID (để xử lý lúc mất mạng)
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();
    // Map số 3: Lưu trữ cái trạng thái do User tự quyết định (như DND hay IDLE) để đè lên ONLINE mặc định
    private final Map<Long, UserStatus> customStatuses = new ConcurrentHashMap<>();

    @Override
    public void userConnected(Long userId, String sessionId) {
        // Gắn Session này cho User
        sessionToUser.put(sessionId, userId);

        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        // Lấy status từ DB để đưa vào RAM (Nếu có thiết lập DND từ trước)
        if (!customStatuses.containsKey(userId)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getStatus() != null && user.getStatus() != UserStatus.OFFLINE) {
                customStatuses.put(userId, user.getStatus());
            } else {
                customStatuses.put(userId, UserStatus.ONLINE);
            }
        }
        // Phát sóng: "Anh A vừa Online / hoặc đang online mà có DND"
        broadcastStatusUpdate(userId, customStatuses.get(userId));
    }

    @Override
    public Long userDisconnected(String sessionId) {
        Long userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                // Nếu user tắt TẤT CẢ các thiết bị đang kết nối -> Sự thật thì người này mới Offline
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    // Dọn dẹp cả RAM
                    customStatuses.remove(userId);
                    // Cập nhật Database
                    updateDatabaseStatus(userId, UserStatus.OFFLINE);

                    // Phát sóng: "A đã thực sự Mất Kết Nối"
                    broadcastStatusUpdate(userId, UserStatus.OFFLINE);

                    return userId;
                }
            }
        }
        return null;
    }

    @Override
    public void updateUserStatus(Long userId, UserStatus newStatus) {
        if (newStatus == UserStatus.OFFLINE) {
            // Không cho phép tự set Offline (Ẩn danh thường hay làm phức tạp hệ thống ban đầu,
            // nên tạm gác qua tính năng Ẩn danh - Offline do Server tự quyết)
            return;
        }
        // Đang không online trên bất kỳ thiết bị nào thì không cho chỉnh
        if (!userSessions.containsKey(userId)) {
            return;
        }
        customStatuses.put(userId, newStatus);

        // Cập nhật DB
        updateDatabaseStatus(userId, newStatus);
        // Phát sóng cho mọi người biết
        broadcastStatusUpdate(userId, newStatus);
    }

    @Override
    public UserStatus getUserStatus(Long userId) {
        // Nếu không có session nào đang chạy => OFFLINE
        if (!userSessions.containsKey(userId) || userSessions.get(userId).isEmpty()) {
            return UserStatus.OFFLINE;
        }

        // Trả về DND hoặc IDLE nếu có, không thì cứ ONLINE
        return customStatuses.getOrDefault(userId, UserStatus.ONLINE);
    }

    private void updateDatabaseStatus(Long userId, UserStatus status) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getStatus() != status) {
                user.setStatus(status);
                userRepository.save(user);
            }
        } catch (Exception e) {
            log.error("Failed to update status for user {}", userId, e);
        }
    }

    private void broadcastStatusUpdate(Long userId, UserStatus status) {
        PresenceMessage message = PresenceMessage.builder()
                .type("STATUS_UPDATE")
                .userId(userId)
                .status(status)
                .build();
        // Trong tương lai nếu tối ưu, bạn sửa dòng này thành Gửi cho Từng Server/Bạn bè riêng lẻ.
        // Hiện tại: Gửi cho toàn bộ những người đang sub /topic/presence
        messagingTemplate.convertAndSend("/topic/presence", message);
    }
}
