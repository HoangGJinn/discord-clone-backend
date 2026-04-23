package com.discordclone.backend.dto.response;

import com.discordclone.backend.dto.message.MessageAttachment;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class DirectMessageResponse {
    private String id;
    private String conversationId;
    private Long senderId;
    private Long receiverId;
    private UserResponse sender;
    private UserResponse receiver;
    private String content;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Date createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Date updatedAt;

    private boolean edited;
    private boolean deleted;
    private boolean isRead;

    private List<MessageAttachment> attachments;
    private String replyToId;
    private DirectMessageResponse replyToMessage;

    private Map<String, Set<Long>> reactions;
}
