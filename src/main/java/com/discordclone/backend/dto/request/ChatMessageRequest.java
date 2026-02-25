package com.discordclone.backend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ChatMessageRequest {
    private String content;
    private Long senderId;
    private String replyToId;
    private List<String> attachments;
}
