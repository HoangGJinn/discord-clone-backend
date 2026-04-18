package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NitroOrderSummary {
    private Long id;
    private String txnRef;
    private Long userId;
    private String userName;
    private Integer amount;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdAt;
}
