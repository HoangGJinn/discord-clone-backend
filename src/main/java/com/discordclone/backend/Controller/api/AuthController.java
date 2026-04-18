package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.LoginRequest;
import com.discordclone.backend.dto.response.LoginResponse;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.security.jwt.JwtUtils;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.List;
import java.util.List;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest req) {
        try {
            log.info("Login attempt for username: {}", req.getUserName());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUserName(), req.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            log.info("User authenticated: {}", userDetails.getUsername());

            // Load full user to get all details including roles
            Optional<User> userOpt = userService.findByUserName(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                log.warn("User not found after authentication: {}", userDetails.getUsername());
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            User user = userOpt.get();
            log.info("User roles: {}", user.getRoles());

            // Build roles list
            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toList();

            // Allow admins to login as regular users in the mobile app

            return ResponseEntity.ok(LoginResponse.builder()
                    .message("Login thành công")
                    .token(jwt)
                    .userId(user.getId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .roles(roles)
                    .build());
        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.warn("Login failed for username {}: Sai tài khoản hoặc mật khẩu.", req.getUserName());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Sai tài khoản hoặc mật khẩu."));
        } catch (Exception ex) {
            log.error("Login failed for username {} due to server error: {}", req.getUserName(), ex.getMessage(), ex);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Đã xảy ra lỗi máy chủ."));
        }
    }
}


