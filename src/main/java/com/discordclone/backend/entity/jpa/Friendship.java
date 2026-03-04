package com.discordclone.backend.entity.jpa;

import com.discordclone.backend.entity.enums.FriendshipStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Bảng friendship lưu mối quan hệ bạn bè giữa 2 user.
 * - sender: người gửi lời mời kết bạn
 * - receiver: người nhận lời mời
 * - status: PENDING / ACCEPTED / REJECTED / BLOCKED
 *
 * Unique constraint đảm bảo mỗi cặp (sender, receiver) chỉ có 1 record.
 */
@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(columnNames = { "sender_id", "receiver_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
