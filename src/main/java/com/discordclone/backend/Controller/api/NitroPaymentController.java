package com.discordclone.backend.Controller.api;

import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.payment.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class NitroPaymentController {
    private final VnpayService vnpayService;

    @PostMapping("/nitro/create")
    public ResponseEntity<?> createPayment(@AuthenticationPrincipal UserDetailsImpl user,
                                           @RequestParam(defaultValue = "50000") Long amount,
                                           HttpServletRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        if (amount == null || amount <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Amount must be greater than 0"));
        }

        String paymentUrl = vnpayService.createPaymentUrl(user.getId(), amount, request);
        return ResponseEntity.ok(Map.of("url", paymentUrl));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> ipnListener(@RequestParam Map<String, String> params) {
        Map<String, String> response = vnpayService.processIpn(params);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/nitro/confirm-mobile")
    public ResponseEntity<?> confirmMobilePayment(@AuthenticationPrincipal UserDetailsImpl user,
                                                  @RequestBody Map<String, String> params) {
        Long userId = user != null ? user.getId() : null;
        Map<String, String> response = vnpayService.processMobileReturn(userId, params);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{txnRef}")
    public ResponseEntity<?> getOrderStatus(@PathVariable String txnRef) {
        return ResponseEntity.ok(vnpayService.getOrderStatus(txnRef));
    }
}
