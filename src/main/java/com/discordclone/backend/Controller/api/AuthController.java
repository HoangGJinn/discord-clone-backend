package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.ForgotPasswordRequest;
import com.discordclone.backend.dto.request.LoginRequest;
import com.discordclone.backend.dto.request.ResetPasswordRequest;
import com.discordclone.backend.dto.response.LoginResponse;
import com.discordclone.backend.entity.jpa.PasswordResetOtp;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.PasswordResetOtpRepository;
import com.discordclone.backend.service.jwt.JwtService;
import com.discordclone.backend.service.otp.OtpService;
import com.discordclone.backend.service.user.UserService;

import com.discordclone.backend.service.email.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

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

    @PostMapping("/forget-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            // Kiểm tra Email có tồn tại không
            User user = userService.findByEmail(req.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

            otpService.generateAndSendOtp(req.getEmail(), "RESET_PASSWORD");
            return ResponseEntity.ok(Map.of(
                    "message", "Mã OTP đã được gửi đến email của bạn",
                    "email", req.getEmail()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            // Tìm OTP trong DB
            PasswordResetOtp otpEntity = otpService.verifyOtp(req.getEmail(), req.getOtp());

            User user = userService.findByEmail(req.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email không tồn tại"));
            userService.updatePassword(user, req.getNewPassword());

            // Đánh dấu OTP đã dùng
            otpService.markOtpAsUsed(otpEntity);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }
}
