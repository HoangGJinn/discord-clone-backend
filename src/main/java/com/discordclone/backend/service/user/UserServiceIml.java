package com.discordclone.backend.service.user;

import com.discordclone.backend.dto.request.RegisterRequest;
import com.discordclone.backend.dto.request.UpdateProfileRequest;
import com.discordclone.backend.entity.enums.ERole;
import com.discordclone.backend.entity.jpa.Role;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.RoleRepository;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.service.otp.OtpService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceIml implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final OtpService otpService;

    @Override
    public Optional<User> findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void registerUser(RegisterRequest request) {
        if (userRepository.existsByUserName(request.getUsername()))
            throw new IllegalArgumentException("Username already exists");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email already exists");

        Role defaultRole = roleRepository.findByName(ERole.USER_DEFAULT)
                .orElseThrow(() -> new EntityNotFoundException("Role user not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);

        User user = User.builder()
                .userName(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .displayName(request.getDisplayName())
                .isActive(false)
                .isEmailVerified(false)
                .roles(roles)
                .build();

        userRepository.save(user);

        // Sinh OTP
        otpService.generateAndSendOtp(user.getEmail(), "VERIFY_ACCOUNT");
    }

    @Override
    public void verifyAccount(String email, String otp) {
        // 1. Kiểm tra OTP có đúng không (Gọi sang OtpService)
        var otpEntity = otpService.verifyOtp(email, otp);

        // 2. Tìm User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        // 3. Kích hoạt tài khoản
        user.setIsActive(true);
        user.setIsEmailVerified(true);
        userRepository.save(user);

        // 4. Đánh dấu OTP đã dùng
        otpService.markOtpAsUsed(otpEntity);
    }

    @Override
    public User updateProfile(String userName, UpdateProfileRequest request) {
        User user = findByUserName(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userName));

        if (request.getDisplayName() != null)
            user.setDisplayName(request.getDisplayName());
        if (request.getBio() != null)
            user.setBio(request.getBio());
        if (request.getAvatarUrl() != null)
            user.setAvatarUrl(request.getAvatarUrl());
        if (request.getPronouns() != null)
            user.setPronouns(request.getPronouns());
        if (request.getCountry() != null)
            user.setCountry(request.getCountry());
        if (request.getBirthDate() != null)
            user.setBirthDate(request.getBirthDate());

        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
