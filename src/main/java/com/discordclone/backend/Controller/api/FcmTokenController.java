package com.discordclone.backend.Controller.api;

import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.entity.jpa.UserFcmToken;
import com.discordclone.backend.repository.UserFcmTokenRepository;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Endpoints quản lý FCM token:
 *  POST   /api/fcm/token  — đăng ký / cập nhật token khi login hoặc token refresh
 *  DELETE /api/fcm/token  — xóa token khi logout
 */
@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
@Slf4j
public class FcmTokenController {

    private final UserFcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * Client gọi API này ngay sau khi login thành công hoặc khi FCM token refresh.
     *
     * Body: { "deviceId": "uuid-thiết-bị", "fcmToken": "token-từ-firebase" }
     */
    @PostMapping("/token")
    @Transactional
    public ResponseEntity<?> registerToken(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestBody Map<String, String> body) {
        try {
            String deviceId = body.get("deviceId");
            String fcmToken = body.get("fcmToken");

            if (deviceId == null || deviceId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "deviceId is required"));
            }
            if (fcmToken == null || fcmToken.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "fcmToken is required"));
            }

            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Upsert: nếu đã có token cho deviceId này thì cập nhật, chưa có thì tạo mới
            Optional<UserFcmToken> existing = fcmTokenRepository
                    .findByUserIdAndDeviceId(user.getId(), deviceId);

            if (existing.isPresent()) {
                UserFcmToken token = existing.get();
                if (!token.getFcmToken().equals(fcmToken)) {
                    token.setFcmToken(fcmToken);
                    fcmTokenRepository.save(token);
                    log.info("[FCM] Updated token for user={} device={}", user.getId(), deviceId);
                }
            } else {
                UserFcmToken newToken = UserFcmToken.builder()
                        .user(user)
                        .deviceId(deviceId)
                        .fcmToken(fcmToken)
                        .build();
                fcmTokenRepository.save(newToken);
                log.info("[FCM] Registered new token for user={} device={}", user.getId(), deviceId);
            }

            return ResponseEntity.ok(Map.of("message", "FCM token registered successfully"));
        } catch (Exception e) {
            log.error("[FCM] Token registration failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to register FCM token"));
        }
    }

    /**
     * Client gọi API này khi logout.
     * Body: { "deviceId": "uuid-thiết-bị" }
     *
     * Bắt buộc phải xóa token để tránh user khác nhận nhầm notification.
     */
    @DeleteMapping("/token")
    @Transactional
    public ResponseEntity<?> removeToken(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestBody Map<String, String> body) {
        try {
            String deviceId = body.get("deviceId");

            if (deviceId == null || deviceId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "deviceId is required"));
            }

            fcmTokenRepository.deleteByUserIdAndDeviceId(currentUser.getId(), deviceId);
            log.info("[FCM] Removed token for user={} device={}", currentUser.getId(), deviceId);

            return ResponseEntity.ok(Map.of("message", "FCM token removed successfully"));
        } catch (Exception e) {
            log.error("[FCM] Token removal failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to remove FCM token"));
        }
    }
}
