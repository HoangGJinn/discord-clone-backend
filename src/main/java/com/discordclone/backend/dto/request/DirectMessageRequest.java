package com.discordclone.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DirectMessageRequest {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    private String content;

    private List<String> attachments;

    private String replyToId;
}
