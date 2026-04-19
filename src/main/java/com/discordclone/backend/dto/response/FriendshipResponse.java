package com.discordclone.backend.dto.response;

import com.discordclone.backend.entity.enums.FriendshipStatus;
import com.discordclone.backend.entity.enums.UserStatus;
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
    private UserStatus senderStatus;
    private String senderAvatarEffectId;
    private String senderBannerEffectId;
    private String senderCardEffectId;

    // Thông tin người nhận
    private Long receiverId;
    private String receiverUsername;
    private String receiverDisplayName;
    private String receiverAvatarUrl;
    private UserStatus receiverStatus;
    private String receiverAvatarEffectId;
    private String receiverBannerEffectId;
    private String receiverCardEffectId;

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
                .senderStatus(sender.getStatus())
                .senderAvatarEffectId(sender.getAvatarEffectId())
                .senderBannerEffectId(sender.getBannerEffectId())
                .senderCardEffectId(sender.getCardEffectId())
                .receiverId(receiver.getId())
                .receiverUsername(receiver.getUserName())
                .receiverDisplayName(receiver.getDisplayName())
                .receiverAvatarUrl(receiver.getAvatarUrl())
                .receiverStatus(receiver.getStatus())
                .receiverAvatarEffectId(receiver.getAvatarEffectId())
                .receiverBannerEffectId(receiver.getBannerEffectId())
                .receiverCardEffectId(receiver.getCardEffectId())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
