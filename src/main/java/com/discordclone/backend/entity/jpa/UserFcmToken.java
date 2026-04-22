package com.discordclone.backend.entity.jpa;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Lưu FCM token của mỗi thiết bị (device) của user.
 * Một user có thể đăng nhập trên nhiều thiết bị → nhiều token.
 * deviceId giúp phân biệt thiết bị, tránh trùng lặp token.
 */
@Entity
@Table(
    name = "user_fcm_tokens",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Định danh thiết bị — client tự tạo (UUID), lưu vào AsyncStorage.
     * Giúp cập nhật token đúng thiết bị khi token xoay vòng.
     */
    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    /**
     * FCM Registration Token — do Firebase SDK cấp cho thiết bị.
     * Có thể thay đổi theo thời gian (FCM token refresh).
     */
    @Column(name = "fcm_token", nullable = false, columnDefinition = "TEXT")
    private String fcmToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
