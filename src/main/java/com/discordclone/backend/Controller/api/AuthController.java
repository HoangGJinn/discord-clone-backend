package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.ForgotPasswordRequest;
import com.discordclone.backend.dto.request.LoginRequest;
import com.discordclone.backend.dto.request.RegisterRequest;
import com.discordclone.backend.dto.request.ResendOtpRequest;
import com.discordclone.backend.dto.request.ResetPasswordRequest;
import com.discordclone.backend.dto.request.VerifyAccountRequest;
import com.discordclone.backend.dto.response.LoginResponse;
import com.discordclone.backend.entity.jpa.PasswordResetOtp;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.exception.AccountNotActiveException;
import com.discordclone.backend.exception.AccountNotVerifiedException;
import com.discordclone.backend.security.jwt.JwtUtils;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.otp.OtpService;
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
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Register successful. Please check your email for OTP.",
                    "email", request.getEmail()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "System error: " + ex.getMessage()));
        }
    }

    @PostMapping("/verify-account")
    public ResponseEntity<?> verifyAccount(@Valid @RequestBody VerifyAccountRequest request) {
        try {
            userService.verifyAccount(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(
                    Map.of("message", "Account verified successfully. You can login now."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        try {
            if (!request.getType().equals("VERIFY_ACCOUNT") && !request.getType().equals("RESET_PASSWORD")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid type. Allowed values: VERIFY_ACCOUNT, RESET_PASSWORD"));
            }

            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email does not exist in system"));

            if (request.getType().equals("VERIFY_ACCOUNT")
                    && user.getIsEmailVerified() != null
                    && user.getIsEmailVerified()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Account is already verified."));
            }

            otpService.generateAndSendOtp(request.getEmail(), request.getType());

            return ResponseEntity.ok(Map.of(
                    "message", "A new OTP has been sent to your email",
                    "email", request.getEmail()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

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

            Optional<User> userOpt = userService.findByUserName(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                log.warn("User not found after authentication: {}", userDetails.getUsername());
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            User user = userOpt.get();
            log.info("User roles: {}", user.getRoles());

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toList();

            return ResponseEntity.ok(LoginResponse.builder()
                    .message("Login successful")
                    .token(jwt)
                    .userId(user.getId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .roles(roles)
                    .build());
        } catch (AccountNotVerifiedException ex) {
            return ResponseEntity.status(403).body(Map.of("message", ex.getMessage()));
        } catch (AccountNotActiveException ex) {
            return ResponseEntity.status(403).body(Map.of("message", ex.getMessage()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.warn("Login failed for username {}: invalid credentials.", req.getUserName());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid username or password."));
        } catch (Exception ex) {
            log.error("Login failed for username {} due to server error: {}", req.getUserName(), ex.getMessage(), ex);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Server error."));
        }
    }

    @PostMapping("/forget-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            userService.findByEmail(req.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email does not exist in system"));

            otpService.generateAndSendOtp(req.getEmail(), "RESET_PASSWORD");
            return ResponseEntity.ok(Map.of(
                    "message", "OTP has been sent to your email",
                    "email", req.getEmail()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            PasswordResetOtp otpEntity = otpService.verifyOtp(req.getEmail(), req.getOtp());

            User user = userService.findByEmail(req.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email does not exist"));
            userService.updatePassword(user, req.getNewPassword());

            otpService.markOtpAsUsed(otpEntity);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
