package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummary {
    private Long userId;
    private String userName;
    private String displayName;
    private String email;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private java.time.LocalDateTime createdAt;
    private Integer serverCount;
    private Integer friendCount;
}
