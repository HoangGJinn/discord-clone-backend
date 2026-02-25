package com.discordclone.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String content;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private LocalDateTime createdAt;
}
