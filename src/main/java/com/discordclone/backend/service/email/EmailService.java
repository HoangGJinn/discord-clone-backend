package com.discordclone.backend.service.email;

public interface EmailService {
    /**
     * Gửi OTP xác thực tài khoản hoặc lấy lại mật khẩu
     * @param toEmail Email người nhận
     * @param otp Mã xác thực (6 số)
     * @param type Loại email: "VERIFY_ACCOUNT" hoặc "RESET_PASSWORD"
     */
    void sendOTPToEmail(String toEmail, String otp, String type);

    // Sau này nếu Discord Clone có tính năng thông báo tin nhắn offline
    // hoặc thông báo lời mời kết bạn qua mail thì thêm vào đây sau.
}
