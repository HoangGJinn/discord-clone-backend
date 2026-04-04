package com.discordclone.backend.dto.response;

import com.discordclone.backend.entity.enums.MemberRole;
import com.discordclone.backend.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerMemberResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String displayName;
    private String nickname;
    private String avatarUrl;
    private UserStatus status;
    private MemberRole role;
    private LocalDateTime joinedAt;
}
