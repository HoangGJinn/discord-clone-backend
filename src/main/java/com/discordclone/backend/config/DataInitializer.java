package com.discordclone.backend.config;

import com.discordclone.backend.entity.jpa.Role;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.entity.enums.ERole;
import com.discordclone.backend.repository.RoleRepository;
import com.discordclone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) throws Exception {
                // Ensure ADMIN role exists
                Role adminRole = roleRepository.findByName(ERole.ADMIN)
                                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.ADMIN).build()));

                Role userRole = roleRepository.findByName(ERole.USER_DEFAULT)
                                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.USER_DEFAULT).build()));

                // Check if any admin user exists
                if (userRepository.findByUserName("admin").isEmpty()) {
                        User admin = User.builder()
                                        .userName("admin")
                                        .email("admin@discord.com")
                                        .password(passwordEncoder.encode("admin123"))
                                        .displayName("Administrator")
                                        .isActive(true)
                                        .isEmailVerified(true)
                                        .build();
                        Set<Role> roles = new HashSet<>();
                        roles.add(adminRole);
                        admin.setRoles(roles);
                        userRepository.save(admin);
                        System.out.println("Default admin user created: admin / admin123");
                }

                // Create a test user if none exists
                if (userRepository.findByUserName("testuser").isEmpty()) {
                        User testUser = User.builder()
                                        .userName("testuser")
                                        .email("test@discord.com")
                                        .password(passwordEncoder.encode("test123"))
                                        .displayName("Test User")
                                        .isActive(true)
                                        .isEmailVerified(true)
                                        .build();
                        Set<Role> roles = new HashSet<>();
                        roles.add(userRole);
                        testUser.setRoles(roles);
                        userRepository.save(testUser);
                        System.out.println("Test user created: testuser / test123");
                }
        }
}
