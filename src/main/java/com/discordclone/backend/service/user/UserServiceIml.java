package com.discordclone.backend.service.user;

import com.discordclone.backend.dto.request.RegisterRequest;
import com.discordclone.backend.entity.enums.ERole;
import com.discordclone.backend.entity.jpa.Role;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.RoleRepository;
import com.discordclone.backend.repository.UserRepository;
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
                .isActive(true)
                .isEmailVerified(false)
                .roles(roles)
                .build();

        userRepository.save(user);
    }
}
