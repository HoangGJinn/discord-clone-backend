package com.discordclone.backend.dto.request;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String content;
    private Long senderId;
}
