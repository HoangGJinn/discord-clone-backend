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
        private final com.discordclone.backend.repository.ProfileEffectRepository profileEffectRepository;

        @Override
        public void run(String... args) throws Exception {
                // Ensure roles
                Role adminRole = roleRepository.findByName(ERole.ADMIN)
                                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.ADMIN).build()));

                Role userRole = roleRepository.findByName(ERole.USER_DEFAULT)
                                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.USER_DEFAULT).build()));

                // Initialize users
                initializeUsers(adminRole, userRole);
                
                // Initialize profile effects
                initializeProfileEffects();
        }

        private void initializeUsers(Role adminRole, Role userRole) {
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

        private void initializeProfileEffects() {
                if (profileEffectRepository.count() == 0) {
                        var effects = java.util.List.of(
                                com.discordclone.backend.entity.jpa.ProfileEffect.builder()
                                        .name("Cyberpunk Core")
                                        .description("Gói hiệu ứng công nghệ tương lai với tông màu tím neon.")
                                        .imageUrl("https://raw.githubusercontent.com/LottieFiles/core-lottie-docs/main/assets/lottie_logo.png")
                                        .animationUrl("https://assets2.lottiefiles.com/packages/lf20_m6cuL6.json")
                                        .price(199000.0)
                                        .type("BANNER")
                                        .isActive(true)
                                        .build(),
                                com.discordclone.backend.entity.jpa.ProfileEffect.builder()
                                        .name("Hỏa Long")
                                        .description("Hiệu ứng lửa rực cháy quanh avatar cực ngầu.")
                                        .imageUrl("https://cdn-icons-png.flaticon.com/512/426/426833.png")
                                        .animationUrl("https://assets9.lottiefiles.com/packages/lf20_S69rU9.json")
                                        .price(150000.0)
                                        .type("AVATAR")
                                        .isActive(true)
                                        .build(),
                                com.discordclone.backend.entity.jpa.ProfileEffect.builder()
                                        .name("Hoa Anh Đào")
                                        .description("Cánh hoa rơi nhẹ nhàng, mang lại cảm giác yên bình.")
                                        .imageUrl("https://cdn-icons-png.flaticon.com/512/1087/1087431.png")
                                        .animationUrl("https://assets10.lottiefiles.com/packages/lf20_m6vaxmye.json")
                                        .price(99000.0)
                                        .type("CARD")
                                        .isActive(true)
                                        .build(),
                                com.discordclone.backend.entity.jpa.ProfileEffect.builder()
                                        .name("Sóng Biển")
                                        .description("Hiệu ứng nước chuyển động mượt mà.")
                                        .imageUrl("https://cdn-icons-png.flaticon.com/512/2855/2855513.png")
                                        .animationUrl("https://assets4.lottiefiles.com/packages/lf20_Yia6m9.json")
                                        .price(120000.0)
                                        .type("BANNER")
                                        .isActive(true)
                                        .build()
                        );
                        profileEffectRepository.saveAll(effects);
                        System.out.println("Sample profile effects initialized: 4 items");
                }
        }
}
