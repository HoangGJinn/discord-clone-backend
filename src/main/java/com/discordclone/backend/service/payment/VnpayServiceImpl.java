package com.discordclone.backend.service.payment;

import com.discordclone.backend.entity.enums.ERole;
import com.discordclone.backend.entity.jpa.NitroOrder;
import com.discordclone.backend.entity.jpa.Role;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.NitroOrderRepository;
import com.discordclone.backend.repository.RoleRepository;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.utils.vnpay.VnpayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VnpayServiceImpl implements VnpayService {

    private final NitroOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${app.vnpay.tmn-code}")
    private String tmnCode;
    @Value("${app.vnpay.hash-secret}")
    private String hashSecret;
    @Value("${app.vnpay.url}")
    private String vnpUrl;
    @Value("${app.vnpay.return-url}")
    private String returnUrl;

    @Override
    @Transactional
    public String createPaymentUrl(Long userId, Long amount, HttpServletRequest request) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = generateUniqueTxnRef();
        String vnp_IpAddr = VnpayUtil.getIpAddress(request);
        String vnp_TmnCode = tmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPAY tính theo xu
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan Nitro cho User ID: " + userId);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", LocalDateTime.now().format(formatter));

        // Lưu đơn hàng vào database ở trạng thái PENDING
        NitroOrder nitroOrder = NitroOrder.builder()
                .vnpTxnRef(vnp_TxnRef)
                .userId(userId)
                .amount(amount)
                .status("PENDING")
                .build();
        orderRepository.save(nitroOrder);

        // Xử lý tạo URL và chữ ký
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnpayUtil.hmacSHA512(hashSecret, hashData.toString());
        return vnpUrl + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;
    }

    @Override
    @Transactional
    public Map<String, String> processIpn(Map<String, String> params) {
        if (!validateSignature(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid Checksum");
        }

        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        String vnp_TmnCode = params.get("vnp_TmnCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_Amount = params.get("vnp_Amount");

        if (vnp_TxnRef == null || vnp_TxnRef.isBlank()) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        if (!tmnCode.equals(vnp_TmnCode)) {
            return Map.of("RspCode", "97", "Message", "Invalid TmnCode");
        }

        NitroOrder order = orderRepository.findByVnpTxnRef(vnp_TxnRef)
                .orElse(null);

        if (order == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        long ipnAmount;
        try {
            ipnAmount = Long.parseLong(vnp_Amount);
        } catch (Exception e) {
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }

        if (ipnAmount != order.getAmount() * 100) {
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }

        if ("SUCCESS".equals(order.getStatus())) {
            return Map.of("RspCode", "02", "Message", "Order already confirmed");
        }

        if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
            order.setStatus("SUCCESS");
            upgradeUserToNitro(order.getUserId(), order.getId());
        } else {
            order.setStatus("FAILED");
        }
        orderRepository.save(order);

        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }

    @Override
    @Transactional
    public Map<String, String> processMobileReturn(Long userId, Map<String, String> params) {
        if (!validateSignature(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid Checksum");
        }

        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        String vnp_TmnCode = params.get("vnp_TmnCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_Amount = params.get("vnp_Amount");

        if (vnp_TxnRef == null || vnp_TxnRef.isBlank()) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        NitroOrder order = orderRepository.findByVnpTxnRef(vnp_TxnRef)
                .orElse(null);
        if (order == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        if (userId != null && !Objects.equals(order.getUserId(), userId)) {
            return Map.of("RspCode", "03", "Message", "Order does not belong to current user");
        }

        if (!tmnCode.equals(vnp_TmnCode)) {
            return Map.of("RspCode", "97", "Message", "Invalid TmnCode");
        }

        long returnedAmount;
        try {
            returnedAmount = Long.parseLong(vnp_Amount);
        } catch (Exception e) {
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }

        if (returnedAmount != order.getAmount() * 100) {
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }

        if ("SUCCESS".equals(order.getStatus())) {
            return Map.of("RspCode", "02", "Message", "Order already confirmed");
        }

        if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
            order.setStatus("SUCCESS");
            upgradeUserToNitro(order.getUserId(), order.getId());
            orderRepository.save(order);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }

        order.setStatus("FAILED");
        orderRepository.save(order);
        return Map.of("RspCode", "24", "Message", "Payment failed or canceled");
    }

    @Override
    @Transactional
    public void upgradeUserToNitro(Long userId, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role premiumRole = roleRepository.findByName(ERole.USER_PREMIUM)
            .orElseGet(() -> roleRepository.save(Role.builder()
                .name(ERole.USER_PREMIUM)
                .description("Premium user role")
                .build()));

        boolean alreadyPremium = user.getRoles().stream()
            .anyMatch(role -> role.getName() == ERole.USER_PREMIUM);
        if (alreadyPremium) {
            return;
        }

        user.getRoles().add(premiumRole);
        userRepository.save(user);
    }

    @Override
    public boolean validateSignature(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }

        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isBlank()) {
            return false;
        }

        Map<String, String> validParams = new HashMap<>(params);
        validParams.remove("vnp_SecureHash");
        validParams.remove("vnp_SecureHashType");

        String signData = VnpayUtil.getPaymentURL(validParams, false);
        String calculatedHash = VnpayUtil.hmacSHA512(hashSecret, signData);

        return vnpSecureHash.equalsIgnoreCase(calculatedHash);
    }

    private String generateUniqueTxnRef() {
        String txnRef;
        do {
            txnRef = VnpayUtil.getRandomNumber(8);
        } while (orderRepository.findByVnpTxnRef(txnRef).isPresent());

        return txnRef;
    }

    @Override
    public Map<String, Object> getOrderStatus(String txnRef) {
        return orderRepository.findByVnpTxnRef(txnRef)
                .map(order -> Map.<String, Object>of(
                        "id", order.getId(),
                        "vnpTxnRef", order.getVnpTxnRef(),
                        "userId", order.getUserId(),
                        "amount", order.getAmount(),
                        "status", order.getStatus(),
                        "createdAt", order.getCreatedAt()
                ))
                .orElse(Map.of("error", "Order not found"));
    }
}