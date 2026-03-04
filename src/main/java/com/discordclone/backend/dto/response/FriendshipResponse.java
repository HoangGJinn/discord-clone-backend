package com.discordclone.backend.dto.response;

import com.discordclone.backend.entity.enums.FriendshipStatus;
import com.discordclone.backend.entity.jpa.Friendship;
import com.discordclone.backend.entity.jpa.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {

    private Long id;
    private FriendshipStatus status;

    // Thông tin người gửi
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;

    // Thông tin người nhận
    private Long receiverId;
    private String receiverUsername;
    private String receiverDisplayName;
    private String receiverAvatarUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Chuyển đổi từ entity sang DTO.
     * Ngoài ra cung cấp thông tin "bạn bè" (người còn lại) dựa vào currentUserId.
     */
    public static FriendshipResponse from(Friendship f) {
        User sender = f.getSender();
        User receiver = f.getReceiver();

        return FriendshipResponse.builder()
                .id(f.getId())
                .status(f.getStatus())
                .senderId(sender.getId())
                .senderUsername(sender.getUserName())
                .senderDisplayName(sender.getDisplayName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .receiverId(receiver.getId())
                .receiverUsername(receiver.getUserName())
                .receiverDisplayName(receiver.getDisplayName())
                .receiverAvatarUrl(receiver.getAvatarUrl())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
