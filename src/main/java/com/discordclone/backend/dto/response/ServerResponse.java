package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse {

    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private String inviteCode;

    // Owner info
    private Long ownerId;
    private String ownerName;

    // Counts
    private Integer memberCount;
    private Integer channelCount;
    private Long unreadCount;

    // Nested data (optional - có thể null nếu chỉ lấy info cơ bản)
    private List<CategoryResponse> categories;
    private List<ChannelResponse> channels; // Channels không thuộc category
    private List<ServerMemberResponse> members;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
