package com.discordclone.backend.service;

import com.discordclone.backend.dto.ProfileEffectDto;
import java.util.List;

public interface ProfileEffectService {
    ProfileEffectDto createProfileEffect(ProfileEffectDto effectDto);
    ProfileEffectDto updateProfileEffect(Long id, ProfileEffectDto effectDto);
    void deleteProfileEffect(Long id);
    ProfileEffectDto getProfileEffectById(Long id);
    List<ProfileEffectDto> getAllProfileEffects();
    List<ProfileEffectDto> getActiveProfileEffects();
    ProfileEffectDto toggleEffectStatus(Long id);
}
