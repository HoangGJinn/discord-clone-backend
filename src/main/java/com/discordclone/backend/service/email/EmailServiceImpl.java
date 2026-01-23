package com.discordclone.backend.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@Slf4j // Tự động có biến log để in ra console
@EnableAsync
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Lấy tên người gửi từ file application.properties (nếu có cấu hình)
    @Value("${spring.mail.username}")
    private String senderEmail;

    @Async // Chạy ngầm để không làm đơ ứng dụng khi đang gửi mail
    @Override
    public void sendOTPToEmail(String toEmail, String otp, String type) {
        try {
            log.info("📧 Bắt đầu gửi email {} tới {}", type, toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);

            // Xác định Tiêu đề và Nội dung dựa trên loại email
            String subject;
            String htmlContent;

            if ("VERIFY_ACCOUNT".equals(type)) {
                subject = "Xác thực tài khoản Discord Clone của bạn";
                htmlContent = getVerifyAccountHtml(otp);
            } else {
                subject = "Đặt lại mật khẩu Discord Clone";
                htmlContent = getResetPasswordHtml(otp);
            }

            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = gửi dưới dạng HTML

            mailSender.send(message);
            log.info("✅ Gửi mail thành công tới: {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Lỗi khi gửi mail tới {}: {}", toEmail, e.getMessage());
        }
    }

    // --- CÁC MẪU EMAIL (HTML STRING) ---
    // Mình thiết kế lại theo phong cách Discord (Tông màu #5865F2)

    private String getVerifyAccountHtml(String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #2f3136; margin: 0; padding: 0;">
                    <div style="max-width: 480px; margin: 40px auto; background-color: #36393f; padding: 40px; border-radius: 8px; text-align: center; color: #dcddde;">
                        <h2 style="color: #5865F2; margin-bottom: 30px;">Chào mừng đến với Discord Clone!</h2>
                        <p style="font-size: 16px; line-height: 1.5;">Cảm ơn bạn đã đăng ký. Vui lòng sử dụng mã bên dưới để xác thực tài khoản của bạn:</p>

                        <div style="margin: 30px 0;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #ffffff; background-color: #5865F2; padding: 10px 20px; border-radius: 4px;">%s</span>
                        </div>

                        <p style="font-size: 14px; color: #72767d;">Mã này sẽ hết hạn trong 5 phút.</p>
                        <p style="font-size: 12px; color: #72767d; margin-top: 40px;">Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>
                    </div>
                </body>
                </html>
                """
                .formatted(otp);
    }

    private String getResetPasswordHtml(String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #2f3136; margin: 0; padding: 0;">
                    <div style="max-width: 480px; margin: 40px auto; background-color: #36393f; padding: 40px; border-radius: 8px; text-align: center; color: #dcddde;">
                        <h2 style="color: #ED4245; margin-bottom: 30px;">Yêu cầu đặt lại mật khẩu</h2>
                        <p style="font-size: 16px; line-height: 1.5;">Chúng tôi nhận được yêu cầu đổi mật khẩu cho tài khoản của bạn. Đây là mã xác nhận:</p>

                        <div style="margin: 30px 0;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #ffffff; background-color: #ED4245; padding: 10px 20px; border-radius: 4px;">%s</span>
                        </div>

                        <p style="font-size: 14px; color: #72767d;">Mã này sẽ hết hạn trong 5 phút.</p>
                        <p style="font-size: 12px; color: #72767d; margin-top: 40px;">Đừng chia sẻ mã này cho bất kỳ ai.</p>
                    </div>
                </body>
                </html>
                """
                .formatted(otp);
    }
}
