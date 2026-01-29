package com.discordclone.backend.service.otp;

import com.discordclone.backend.entity.jpa.PasswordResetOtp;
import com.discordclone.backend.repository.PasswordResetOtpRepository;
import com.discordclone.backend.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpServiceIml implements OtpService{
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void generateAndSendOtp(String email, String type) {
        // Xóa OTP cũ nếu có
        otpRepository.deleteByEmail(email);

        String otp = String.format("%06d", new Random().nextInt(999999));

        PasswordResetOtp otpEntity = PasswordResetOtp.builder()
                .email(email)
                .otp(otp)
                .expireTime(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        emailService.sendOTPToEmail(email, otp, type);
    }

    @Override
    public PasswordResetOtp verifyOtp(String email, String otp) {
        // Tìm OTP trong DB, chưa dùng
        PasswordResetOtp otpEntity = otpRepository.findByEmailAndOtpAndUsedFalse(email, otp)
                .orElseThrow(() -> new RuntimeException("OTP không hợp lệ hoặc đã được sử dụng"));
        // Kiểm tra thời gian hết hạn
        if (otpEntity.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP đã quá hạn");
        }
        return otpEntity;
    }

    @Override
    public void markOtpAsUsed(PasswordResetOtp otpEntity) {
        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);
    }
}
