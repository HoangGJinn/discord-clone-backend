package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.ProfileEffectDto;
import com.discordclone.backend.service.ProfileEffectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/effects")
@RequiredArgsConstructor
public class ProfileEffectController {

    private final ProfileEffectService profileEffectService;

    @GetMapping
    public ResponseEntity<List<ProfileEffectDto>> getActiveProfileEffects() {
        return ResponseEntity.ok(profileEffectService.getActiveProfileEffects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileEffectDto> getProfileEffectById(@PathVariable Long id) {
        ProfileEffectDto effectDto = profileEffectService.getProfileEffectById(id);
        if (!effectDto.isActive()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(effectDto);
    }
}
