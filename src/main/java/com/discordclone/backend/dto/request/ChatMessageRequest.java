package com.discordclone.backend.dto.request;

import com.discordclone.backend.dto.message.MessageAttachment;
import lombok.Data;

import java.util.List;

@Data
public class ChatMessageRequest {
    private String content;
    private Long senderId;
    private String replyToId;
    private List<MessageAttachment> attachments;
}
