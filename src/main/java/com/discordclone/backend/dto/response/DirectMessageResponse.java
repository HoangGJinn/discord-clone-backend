package com.discordclone.backend.dto.response;

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
    private Date createdAt;
    private Date updatedAt;

    private boolean edited;
    private boolean deleted;
    private boolean isRead;

    private List<String> attachments;
    private String replyToId;
    private DirectMessageResponse replyToMessage;

    private Map<String, Set<Long>> reactions;
}
