package com.discordclone.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import com.discordclone.backend.dto.message.MessageAttachment;
import lombok.Data;

import java.util.List;

@Data
public class DirectMessageRequest {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    private String content;

    private List<MessageAttachment> attachments;

    private String replyToId;
}
