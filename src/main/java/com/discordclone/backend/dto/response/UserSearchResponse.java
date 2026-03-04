package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response gọn cho thông tin 1 user khi tìm kiếm bạn bè.
 * Không lộ thông tin nhạy cảm như email, password.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;

    // Trạng thái mối quan hệ với user đang login
    // null = chưa có mối quan hệ, "PENDING", "ACCEPTED", "REJECTED", "BLOCKED"
    private String friendshipStatus;
    // Id của friendship nếu có (để dùng khi accept/reject)
    private Long friendshipId;
    // true nếu current user là người gửi lời mời
    private Boolean isSender;
}
