package com.discordclone.backend.entity.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "nitro_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NitroOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String vnpTxnRef; // Mã tham chiếu (mã đơn hàng)
    private Long userId;      // Người mua
    private Long amount;      // Số tiền
    private String status;    // PENDING, SUCCESS, FAILED

    @CreationTimestamp
    private LocalDateTime createdAt;
}
