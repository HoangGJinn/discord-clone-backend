package com.discordclone.backend.service.otp;

import com.discordclone.backend.entity.jpa.PasswordResetOtp;

public interface OtpService {
    void generateAndSendOtp(String email, String type);
    PasswordResetOtp verifyOtp(String email, String otp);
    void markOtpAsUsed(PasswordResetOtp otpEntity);
}
