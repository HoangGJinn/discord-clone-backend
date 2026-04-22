package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSearchResult {
    private String id;
    private String content;
    private String senderName;
    private String senderAvatar;
    private String senderAvatarEffectId;
    private Date createdAt;
    /** Chỉ có khi tìm trong DM */
    private String conversationId;
    /** Chỉ có khi tìm trong Channel */
    private Long channelId;
}
