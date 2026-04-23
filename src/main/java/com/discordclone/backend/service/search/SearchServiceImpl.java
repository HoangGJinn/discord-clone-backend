package com.discordclone.backend.service.search;

import com.discordclone.backend.dto.response.SearchResponse;
import com.discordclone.backend.entity.jpa.Category;
import com.discordclone.backend.entity.jpa.Channel;
import com.discordclone.backend.entity.jpa.Server;
import com.discordclone.backend.entity.jpa.ServerMember;
import com.discordclone.backend.repository.CategoryRepository;
import com.discordclone.backend.repository.ChannelRepository;
import com.discordclone.backend.repository.ServerMemberRepository;
import com.discordclone.backend.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final ServerMemberRepository serverMemberRepository;

    @Override
    public SearchResponse searchAll(String keyword, Long serverId) {
        validateKeyword(keyword);

        String searchKeyword = keyword.trim();

        // Search servers
        List<SearchResponse.ServerSearchResult> serverResults = searchServers(searchKeyword);

        // Search channels
        List<SearchResponse.ChannelSearchResult> channelResults = searchChannels(searchKeyword, serverId);

        // Search categories
        List<SearchResponse.CategorySearchResult> categoryResults = searchCategories(searchKeyword, serverId);

        // Search members (only if serverId is provided)
        List<SearchResponse.MemberSearchResult> memberResults = serverId != null
                ? searchMembers(searchKeyword, serverId)
                : List.of();

        return SearchResponse.builder()
                .keyword(searchKeyword)
                .servers(serverResults)
                .channels(channelResults)
                .categories(categoryResults)
                .members(memberResults)
                .build();
    }

    @Override
    public List<SearchResponse.ServerSearchResult> searchServers(String keyword) {
        validateKeyword(keyword);

        return serverRepository
                .searchByKeyword(keyword.trim())
                .stream()
                .map(this::toServerSearchResult)
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchResponse.ChannelSearchResult> searchChannels(String keyword, Long serverId) {
        validateKeyword(keyword);

        List<Channel> channels;
        if (serverId != null) {
            channels = channelRepository.findByServerIdAndNameContainingIgnoreCase(serverId, keyword.trim());
        } else {
            channels = channelRepository.findByNameContainingIgnoreCase(keyword.trim());
        }

        return channels.stream()
                .map(this::toChannelSearchResult)
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchResponse.CategorySearchResult> searchCategories(String keyword, Long serverId) {
        validateKeyword(keyword);

        List<Category> categories;
        if (serverId != null) {
            categories = categoryRepository.findByServerIdAndNameContainingIgnoreCase(serverId, keyword.trim());
        } else {
            categories = categoryRepository.findByNameContainingIgnoreCase(keyword.trim());
        }

        return categories.stream()
                .map(this::toCategorySearchResult)
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchResponse.MemberSearchResult> searchMembers(String keyword, Long serverId) {
        validateKeyword(keyword);

        if (serverId == null) {
            throw new IllegalArgumentException("Server ID is required for member search");
        }

        return serverMemberRepository
                .searchMembersInServer(serverId, keyword.trim())
                .stream()
                .map(this::toMemberSearchResult)
                .collect(Collectors.toList());
    }

    // ==================== Validation ====================

    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Search keyword cannot be empty");
        }
    }

    // ==================== Helper Mapping Methods ====================

    private SearchResponse.ServerSearchResult toServerSearchResult(Server server) {
        return SearchResponse.ServerSearchResult.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .memberCount(server.getMembers() != null ? server.getMembers().size() : 0)
                .build();
    }

    private SearchResponse.ChannelSearchResult toChannelSearchResult(Channel channel) {
        return SearchResponse.ChannelSearchResult.builder()
                .id(channel.getId())
                .name(channel.getName())
                .type(channel.getType() != null ? channel.getType().name() : null)
                .serverId(channel.getServer() != null ? channel.getServer().getId() : null)
                .serverName(channel.getServer() != null ? channel.getServer().getName() : null)
                .categoryId(channel.getCategory() != null ? channel.getCategory().getId() : null)
                .categoryName(channel.getCategory() != null ? channel.getCategory().getName() : null)
                .build();
    }

    private SearchResponse.CategorySearchResult toCategorySearchResult(Category category) {
        return SearchResponse.CategorySearchResult.builder()
                .id(category.getId())
                .name(category.getName())
                .serverId(category.getServer() != null ? category.getServer().getId() : null)
                .serverName(category.getServer() != null ? category.getServer().getName() : null)
                .channelCount(category.getChannels() != null ? category.getChannels().size() : 0)
                .build();
    }

    private SearchResponse.MemberSearchResult toMemberSearchResult(ServerMember member) {
        return SearchResponse.MemberSearchResult.builder()
                .id(member.getId())
                .userId(member.getUser() != null ? member.getUser().getId() : null)
                .userName(member.getUser() != null ? member.getUser().getUserName() : null)
                .displayName(member.getUser() != null ? member.getUser().getDisplayName() : null)
                .nickname(member.getNickname())
                .role(member.getRole() != null ? member.getRole().name() : null)
                .serverId(member.getServer() != null ? member.getServer().getId() : null)
                .serverName(member.getServer() != null ? member.getServer().getName() : null)
                .avatarUrl(member.getUser() != null ? member.getUser().getAvatarUrl() : null)
                .status(member.getUser() != null && member.getUser().getStatus() != null 
                        ? member.getUser().getStatus().name() : null)
                .avatarEffectId(member.getUser() != null ? member.getUser().getAvatarEffectId() : null)
                .bannerEffectId(member.getUser() != null ? member.getUser().getBannerEffectId() : null)
                .cardEffectId(member.getUser() != null ? member.getUser().getCardEffectId() : null)
                .build();
    }
}
