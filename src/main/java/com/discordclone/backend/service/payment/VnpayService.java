package com.discordclone.backend.service.payment;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Service interface xử lý tích hợp thanh toán VNPAY cho gói Nitro.
 */
public interface VnpayService {

    /**
     * Tạo URL thanh toán để redirect sang cổng VNPAY.
     * * @param userId ID của người dùng thực hiện mua Nitro.
     * @param amount Số tiền thanh toán (VNĐ).
     * @param request Đối tượng HttpServletRequest để lấy địa chỉ IP người dùng.
     * @return Chuỗi URL thanh toán đã được băm mã hóa bảo mật.
     */
    String createPaymentUrl(Long userId, Long amount, HttpServletRequest request);

    /**
     * Xử lý dữ liệu gửi về từ VNPAY qua cổng IPN (Server-to-Server).
     * Đây là bước quan trọng nhất để cập nhật trạng thái Nitro và nâng cấp Role.
     * * @param params Toàn bộ các tham số (Map) nhận được từ VNPAY.
     * @return Map chứa mã phản hồi (RspCode) và thông báo (Message) để trả về cho VNPAY.
     */
    Map<String, String> processIpn(Map<String, String> params);

    /**
     * Xử lý callback trả về từ app mobile (sau khi bắt URL return trong WebView).
     * * @param userId ID user đang đăng nhập trên app.
     * @param params Toàn bộ query params được app trích xuất từ URL callback VNPAY.
     * @return Map chứa mã phản hồi và thông điệp xử lý.
     */
    Map<String, String> processMobileReturn(Long userId, Map<String, String> params);

    /**
     * Kiểm tra và nâng cấp Role người dùng sau khi giao dịch thành công.
     * Phương thức này được gọi nội bộ sau khi xác thực dữ liệu IPN thành công.
     * * @param userId ID người dùng cần nâng cấp lên Nitro.
     * @param orderId ID của đơn hàng Nitro tương ứng.
     */
    void upgradeUserToNitro(Long userId, Long orderId);

    /**
     * Xác thực chữ ký bảo mật (Checksum) của dữ liệu gửi từ VNPAY.
     * * @param params Các tham số nhận được.
     * @return true nếu chữ ký hợp lệ, ngược lại false.
     */
    boolean validateSignature(Map<String, String> params);

    /**
     * Lấy trạng thái đơn hàng theo transaction reference.
     * * @param txnRef Mã tham chiếu giao dịch.
     * @return Map chứa order info (id, status, amount, userId, createdAt).
     */
    Map<String, Object> getOrderStatus(String txnRef);
}