package com.discordclone.backend.entity.jpa;

import com.discordclone.backend.entity.enums.ChannelType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChannelType type = ChannelType.TEXT;

    @Column(length = 500)
    private String topic;

    @Column(name = "position")
    @Builder.Default
    private Integer position = 0;

    @Column(name = "bitrate")
    @Builder.Default
    private Integer bitrate = 64000; // 64kbps

    @Column(name = "user_limit")
    @Builder.Default
    private Integer userLimit = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // nullable - channel có thể không thuộc category nào

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
