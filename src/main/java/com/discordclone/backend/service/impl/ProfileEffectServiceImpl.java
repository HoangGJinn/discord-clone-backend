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
    private final com.discordclone.backend.repository.UserRepository userRepository;
    private final com.discordclone.backend.repository.AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public ProfileEffectDto createProfileEffect(ProfileEffectDto effectDto, Long adminId, String ipAddress) {
        ProfileEffect effect = ProfileEffect.builder()
                .name(effectDto.getName())
                .description(effectDto.getDescription())
                .imageUrl(effectDto.getImageUrl())
                .animationUrl(effectDto.getAnimationUrl())
                .price(effectDto.getPrice())
                .type(effectDto.getType() != null ? effectDto.getType() : "AVATAR")
                .isActive(effectDto.isActive())
                .build();
                
        ProfileEffect savedEffect = profileEffectRepository.save(effect);
        
        logAudit(adminId, "CREATE_EFFECT", "EFFECT", savedEffect.getId(), "{\"name\": \"" + savedEffect.getName() + "\"}", ipAddress);
        
        return mapToDto(savedEffect);
    }

    @Override
    @Transactional
    public ProfileEffectDto updateProfileEffect(Long id, ProfileEffectDto effectDto, Long adminId, String ipAddress) {
        ProfileEffect effect = profileEffectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile effect not found with id: " + id));

        effect.setName(effectDto.getName());
        effect.setDescription(effectDto.getDescription());
        effect.setImageUrl(effectDto.getImageUrl());
        effect.setAnimationUrl(effectDto.getAnimationUrl());
        effect.setPrice(effectDto.getPrice());
        effect.setType(effectDto.getType() != null ? effectDto.getType() : "AVATAR");
        effect.setActive(effectDto.isActive());

        ProfileEffect updatedEffect = profileEffectRepository.save(effect);
        
        logAudit(adminId, "UPDATE_EFFECT", "EFFECT", updatedEffect.getId(), "{\"name\": \"" + updatedEffect.getName() + "\"}", ipAddress);
        
        return mapToDto(updatedEffect);
    }

    @Override
    @Transactional
    public void deleteProfileEffect(Long id, Long adminId, String ipAddress) {
        ProfileEffect effect = profileEffectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile effect not found with id: " + id));
        String effectName = effect.getName();
        profileEffectRepository.delete(effect);
        
        logAudit(adminId, "DELETE_EFFECT", "EFFECT", id, "{\"name\": \"" + effectName + "\"}", ipAddress);
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
    public ProfileEffectDto toggleEffectStatus(Long id, Long adminId, String ipAddress) {
        ProfileEffect effect = profileEffectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile effect not found with id: " + id));
        effect.setActive(!effect.isActive());
        ProfileEffect saved = profileEffectRepository.save(effect);
        
        logAudit(adminId, "TOGGLE_EFFECT", "EFFECT", id, "{\"active\": " + saved.isActive() + "}", ipAddress);
        
        return mapToDto(saved);
    }

    private void logAudit(Long adminId, String action, String targetType, Long targetId, String details, String ipAddress) {
        auditLogRepository.save(com.discordclone.backend.entity.jpa.AuditLog.builder()
                .admin(userRepository.getReferenceById(adminId))
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(ipAddress)
                .build());
    }

    private ProfileEffectDto mapToDto(ProfileEffect effect) {
        return ProfileEffectDto.builder()
                .id(effect.getId())
                .name(effect.getName())
                .description(effect.getDescription())
                .imageUrl(effect.getImageUrl())
                .animationUrl(effect.getAnimationUrl())
                .price(effect.getPrice())
                .type(effect.getType())
                .isActive(effect.isActive())
                .createdAt(effect.getCreatedAt())
                .updatedAt(effect.getUpdatedAt())
                .build();
    }
}
