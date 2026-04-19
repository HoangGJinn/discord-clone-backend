package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO cho API search tổng hợp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private String keyword;

    private List<ServerSearchResult> servers;
    private List<ChannelSearchResult> channels;
    private List<CategorySearchResult> categories;
    private List<MemberSearchResult> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerSearchResult {
        private Long id;
        private String name;
        private String description;
        private String iconUrl;
        private Integer memberCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelSearchResult {
        private Long id;
        private String name;
        private String type;
        private Long serverId;
        private String serverName;
        private Long categoryId;
        private String categoryName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySearchResult {
        private Long id;
        private String name;
        private Long serverId;
        private String serverName;
        private Integer channelCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSearchResult {
        private Long id;
        private Long userId;
        private String userName;
        private String displayName;
        private String nickname;
        private String role;
        private Long serverId;
        private String serverName;
        private String avatarUrl;
        private String status;
        private String avatarEffectId;
        private String bannerEffectId;
        private String cardEffectId;
    }
}
