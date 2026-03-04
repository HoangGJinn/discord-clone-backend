package com.discordclone.backend.dto.presence;

import com.discordclone.backend.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceMessage {
    private String type;         // VD: "STATUS_UPDATE"
    private Long userId;
    private UserStatus status;   // Trạng thái hiện tại của người dùng
}
