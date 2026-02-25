package com.discordclone.backend.dto.response;

import com.discordclone.backend.entity.jpa.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
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

    private Set<String> roles; // ADMIN, USER_DEFAULT, USER_PREMIUM

    public static UserResponse from(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUserName())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .birthDate(user.getBirthDate())
                .country(user.getCountry())
                .pronouns(user.getPronouns())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastActive(user.getLastActive())
                .roles(
                        user.getRoles() == null ? Set.of()
                                : user.getRoles().stream()
                                .map(r -> r.getName().name())
                                .collect(Collectors.toSet())
                )
                .build();
    }
}
