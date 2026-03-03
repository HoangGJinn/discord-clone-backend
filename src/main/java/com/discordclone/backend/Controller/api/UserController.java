package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.response.UserResponse;
import com.discordclone.backend.entity.enums.UserStatus;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.service.presence.PresenceService;
import com.discordclone.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.discordclone.backend.dto.request.UpdateProfileRequest;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final PresenceService presenceService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByUserName(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@RequestBody UpdateProfileRequest request, Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User updatedUser = userService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }

    @PutMapping("/me/status")
    public ResponseEntity<?> updateMyStatus(
            @RequestParam Long userId,
            @RequestParam UserStatus status) {
        try {
            // Không cho phép tự ép hệ thống lỗi bằng cách chọn Offline
            if (status == UserStatus.OFFLINE) {
                return ResponseEntity.badRequest().body("Bạn không thể tự chọn Offline. Hãy đóng ứng dụng thay vì thế!");
            }
            // Gọi logic của Presence Service (Lưu DB + Phát sóng cập nhật trạng thái)
            presenceService.updateUserStatus(userId, status);
            return ResponseEntity.ok("Đã cập nhật trạng thái thành " + status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }
}
