package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetail {
    private Long userId;
    private String userName;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private LocalDate birthDate;
    private String country;
    private String pronouns;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;
    private List<String> roles;
    private Integer friendCount;
    private Integer serverCount;
    private Integer warningCount;
    private List<WarningResponse> recentWarnings;
}