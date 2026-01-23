package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.ForgotPasswordRequest;
import com.discordclone.backend.dto.request.LoginRequest;
import com.discordclone.backend.dto.request.RegisterRequest;
import com.discordclone.backend.dto.request.ResetPasswordRequest;
import com.discordclone.backend.dto.response.LoginResponse;
import com.discordclone.backend.dto.response.RegisterResponse;
import com.discordclone.backend.entity.jpa.PasswordResetOtp;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.security.jwt.JwtUtils;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.otp.OtpService;
import com.discordclone.backend.service.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request);

            return ResponseEntity.ok(RegisterResponse.builder()
                    .message("Đăng ký tài khoản thành công")
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Đăng ký thất bại: " + ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUserName(), req.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            return ResponseEntity.ok(LoginResponse.builder()
                    .message("Login thành công")
                    .token(jwt)
                    .userId(userDetails.getId())
                    .userName(userDetails.getUsername())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Đăng nhập thất bại: " + ex.getMessage()));
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
