package com.discordclone.backend.dto.response;

import com.discordclone.backend.entity.enums.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponse {

    private Long id;
    private String name;
    private ChannelType type;
    private String topic;
    private Integer position;
    private Long serverId;
    private Long categoryId;
    private Integer bitrate;
    private Integer userLimit;
    private Long unreadCount;
    private LocalDateTime createdAt;
}
