package com.discordclone.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String pronouns;
    private String country;
    private LocalDate birthDate;
    private String avatarEffectId;
    private String bannerEffectId;
    private String cardEffectId;
}
