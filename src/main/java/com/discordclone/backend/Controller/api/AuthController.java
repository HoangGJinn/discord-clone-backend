package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.LoginRequest;
import com.discordclone.backend.dto.response.LoginResponse;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.service.Jwt.JwtService;
import com.discordclone.backend.service.User.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest req) {

        try {
            User user = userService.findByUserName(req.getUserName())
                    .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng"));

            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng");
            }

            String token = jwtService.generateToken(user);

            return ResponseEntity.ok(LoginResponse.builder()
                    .message("Login thành công")
                    .token(token)
                    .userId(user.getId())
                    .userName(user.getUserName())
                    .build());

        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }
}
