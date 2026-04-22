package com.discordclone.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ConversationResponse {
    private String id;
    private UserResponse participantOne;
    private UserResponse participantTwo;
    private DirectMessageResponse lastMessage;
    private Long user1Id;
    private Long user2Id;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private Long unreadCount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Date createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Date updatedAt;
}
