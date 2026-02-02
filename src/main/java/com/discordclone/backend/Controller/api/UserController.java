package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.response.UserResponse;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.discordclone.backend.dto.request.UpdateProfileRequest;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByUserName(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@RequestBody UpdateProfileRequest request, Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User updatedUser = userService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }
}
