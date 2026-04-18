package com.discordclone.backend.Controller.api.admin;

import com.discordclone.backend.dto.request.AdminLoginRequest;
import com.discordclone.backend.dto.response.LoginResponse;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.security.jwt.JwtUtils;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Load full user entity
            Optional<User> userOpt = userService.findByUserName(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            User user = userOpt.get();

            // Check if user has ADMIN role
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> role.getName().name().equals("ADMIN"));

            if (!isAdmin) {
                return ResponseEntity.status(403).body(Map.of(
                        "message", "Tài khoản không có quyền truy cập Admin Panel",
                        "hasAdminRole", false
                ));
            }

            // Generate JWT token
            String token = jwtUtils.generateJwtToken(authentication);

            // Build response
            return ResponseEntity.ok(LoginResponse.builder()
                    .message("Đăng nhập Admin thành công")
                    .token(token)
                    .userId(user.getId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .roles(user.getRoles().stream().map(role -> role.getName().name()).toList())
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đăng nhập thất bại: " + e.getMessage()));
        }
    }
}
