package com.discordclone.backend.Controller.api.admin;

import com.discordclone.backend.dto.ProfileEffectDto;
import com.discordclone.backend.service.ProfileEffectService;
import com.discordclone.backend.security.services.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/effects")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminProfileEffectController {

    private final ProfileEffectService profileEffectService;

    @GetMapping
    public ResponseEntity<List<ProfileEffectDto>> getAllProfileEffects() {
        return ResponseEntity.ok(profileEffectService.getAllProfileEffects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileEffectDto> getProfileEffectById(@PathVariable Long id) {
        return ResponseEntity.ok(profileEffectService.getProfileEffectById(id));
    }

    @PostMapping
    public ResponseEntity<ProfileEffectDto> createProfileEffect(
            @RequestBody ProfileEffectDto effectDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request) {
        return ResponseEntity.ok(profileEffectService.createProfileEffect(effectDto, userDetails.getId(), request.getRemoteAddr()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileEffectDto> updateProfileEffect(
            @PathVariable Long id,
            @RequestBody ProfileEffectDto effectDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request) {
        return ResponseEntity.ok(profileEffectService.updateProfileEffect(id, effectDto, userDetails.getId(), request.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfileEffect(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request) {
        profileEffectService.deleteProfileEffect(id, userDetails.getId(), request.getRemoteAddr());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ProfileEffectDto> toggleEffectStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request) {
        return ResponseEntity.ok(profileEffectService.toggleEffectStatus(id, userDetails.getId(), request.getRemoteAddr()));
    }
}
