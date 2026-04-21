package com.discordclone.backend.service.impl;

import com.discordclone.backend.dto.ProfileEffectDto;
import com.discordclone.backend.entity.jpa.ProfileEffect;
import com.discordclone.backend.repository.ProfileEffectRepository;
import com.discordclone.backend.service.ProfileEffectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileEffectServiceImpl implements ProfileEffectService {

    private final ProfileEffectRepository profileEffectRepository;

    @Override
    @Transactional
    public ProfileEffectDto createProfileEffect(ProfileEffectDto effectDto) {
        ProfileEffect effect = ProfileEffect.builder()
                .name(effectDto.getName())
                .description(effectDto.getDescription())
                .imageUrl(effectDto.getImageUrl())
                .animationUrl(effectDto.getAnimationUrl())
                .price(effectDto.getPrice())
                .isActive(effectDto.isActive())
                .build();
                
        ProfileEffect savedEffect = profileEffectRepository.save(effect);
        return mapToDto(savedEffect);
    }

    @Override
    @Transactional
    public ProfileEffectDto updateProfileEffect(Long id, ProfileEffectDto effectDto) {
        ProfileEffect effect = profileEffectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile effect not found with id: " + id));

        effect.setName(effectDto.getName());
        effect.setDescription(effectDto.getDescription());
        effect.setImageUrl(effectDto.getImageUrl());
        effect.setAnimationUrl(effectDto.getAnimationUrl());
        effect.setPrice(effectDto.getPrice());
        effect.setActive(effectDto.isActive());

        ProfileEffect updatedEffect = profileEffectRepository.save(effect);
        return mapToDto(updatedEffect);
    }

    @Override
    @Transactional
    public void deleteProfileEffect(Long id) {
        if (!profileEffectRepository.existsById(id)) {
            throw new RuntimeException("Profile effect not found with id: " + id);
        }
        profileEffectRepository.deleteById(id);
    }

    @Override
    public ProfileEffectDto getProfileEffectById(Long id) {
        ProfileEffect effect = profileEffectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile effect not found with id: " + id));
        return mapToDto(effect);
    }

    @Override
    public List<ProfileEffectDto> getAllProfileEffects() {
        return profileEffectRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProfileEffectDto> getActiveProfileEffects() {
        return profileEffectRepository.findByIsActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProfileEffectDto toggleEffectStatus(Long id) {
        ProfileEffect effect = profileEffectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile effect not found with id: " + id));
        effect.setActive(!effect.isActive());
        return mapToDto(profileEffectRepository.save(effect));
    }

    private ProfileEffectDto mapToDto(ProfileEffect effect) {
        return ProfileEffectDto.builder()
                .id(effect.getId())
                .name(effect.getName())
                .description(effect.getDescription())
                .imageUrl(effect.getImageUrl())
                .animationUrl(effect.getAnimationUrl())
                .price(effect.getPrice())
                .isActive(effect.isActive())
                .createdAt(effect.getCreatedAt())
                .updatedAt(effect.getUpdatedAt())
                .build();
    }
}
