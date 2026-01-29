package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findByEmailAndOtpAndUsedFalse(String email, String otp);

    void deleteByEmail(String email);

}
