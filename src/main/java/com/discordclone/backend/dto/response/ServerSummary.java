package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerSummary {
    private Long serverId;
    private String name;
    private String description;
    private String iconUrl;
    private Long ownerId;
    private String ownerName;
    private Integer memberCount;
    private Integer channelCount;
    private LocalDateTime createdAt;
}
