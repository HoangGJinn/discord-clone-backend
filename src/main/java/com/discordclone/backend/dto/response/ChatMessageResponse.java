package com.discordclone.backend.dto.response;

import com.discordclone.backend.entity.mongo.ChannelMessage;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private Long channelId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private List<String> attachments;
    private Boolean edited;
    private Boolean deleted;
    private Boolean pinned;
    private Date createdAt;
    private Date updatedAt;
    private List<ChannelMessage.Reaction> reactions;
}
