package com.discordclone.backend.dto.response;

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
    private Date createdAt;
    private Date updatedAt;
}
