package com.discordclone.backend.service;

import com.discordclone.backend.dto.ProfileEffectDto;
import java.util.List;

public interface ProfileEffectService {
    ProfileEffectDto createProfileEffect(ProfileEffectDto effectDto, Long adminId, String ipAddress);
    ProfileEffectDto updateProfileEffect(Long id, ProfileEffectDto effectDto, Long adminId, String ipAddress);
    void deleteProfileEffect(Long id, Long adminId, String ipAddress);
    ProfileEffectDto getProfileEffectById(Long id);
    List<ProfileEffectDto> getAllProfileEffects();
    List<ProfileEffectDto> getActiveProfileEffects();
    ProfileEffectDto toggleEffectStatus(Long id, Long adminId, String ipAddress);
}
