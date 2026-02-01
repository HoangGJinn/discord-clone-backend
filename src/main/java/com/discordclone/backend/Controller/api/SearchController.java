package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.response.CategoryResponse;
import com.discordclone.backend.dto.response.ChannelResponse;
import com.discordclone.backend.dto.response.SearchResponse;
import com.discordclone.backend.dto.response.ServerMemberResponse;
import com.discordclone.backend.entity.jpa.*;
import com.discordclone.backend.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "Search", description = "APIs tìm kiếm gần đúng (fuzzy search)")
public class SearchController {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final ServerMemberRepository serverMemberRepository;

    /**
     * Tìm kiếm tổng hợp - tìm trong tất cả entities
     */
    @GetMapping
    @Operation(summary = "Tìm kiếm tổng hợp", description = "Tìm kiếm gần đúng trong servers, channels, categories. Sử dụng LIKE %keyword%")
    public ResponseEntity<SearchResponse> searchAll(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @Parameter(description = "ID server (optional - để lọc channels, categories)") @RequestParam(required = false) Long serverId) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String searchKeyword = keyword.trim();

        // Search servers
        List<SearchResponse.ServerSearchResult> serverResults = serverRepository
                .searchByKeyword(searchKeyword)
                .stream()
                .map(this::toServerSearchResult)
                .collect(Collectors.toList());

        // Search channels
        List<SearchResponse.ChannelSearchResult> channelResults;
        if (serverId != null) {
            channelResults = channelRepository
                    .findByServerIdAndNameContainingIgnoreCase(serverId, searchKeyword)
                    .stream()
                    .map(this::toChannelSearchResult)
                    .collect(Collectors.toList());
        } else {
            channelResults = channelRepository
                    .findByNameContainingIgnoreCase(searchKeyword)
                    .stream()
                    .map(this::toChannelSearchResult)
                    .collect(Collectors.toList());
        }

        // Search categories
        List<SearchResponse.CategorySearchResult> categoryResults;
        if (serverId != null) {
            categoryResults = categoryRepository
                    .findByServerIdAndNameContainingIgnoreCase(serverId, searchKeyword)
                    .stream()
                    .map(this::toCategorySearchResult)
                    .collect(Collectors.toList());
        } else {
            categoryResults = categoryRepository
                    .findByNameContainingIgnoreCase(searchKeyword)
                    .stream()
                    .map(this::toCategorySearchResult)
                    .collect(Collectors.toList());
        }

        // Search members (only if serverId is provided)
        List<SearchResponse.MemberSearchResult> memberResults = List.of();
        if (serverId != null) {
            memberResults = serverMemberRepository
                    .searchMembersInServer(serverId, searchKeyword)
                    .stream()
                    .map(this::toMemberSearchResult)
                    .collect(Collectors.toList());
        }

        SearchResponse response = SearchResponse.builder()
                .keyword(searchKeyword)
                .servers(serverResults)
                .channels(channelResults)
                .categories(categoryResults)
                .members(memberResults)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Tìm kiếm servers
     */
    @GetMapping("/servers")
    @Operation(summary = "Tìm kiếm servers", description = "Tìm kiếm gần đúng theo tên hoặc mô tả server")
    public ResponseEntity<List<SearchResponse.ServerSearchResult>> searchServers(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<SearchResponse.ServerSearchResult> results = serverRepository
                .searchByKeyword(keyword.trim())
                .stream()
                .map(this::toServerSearchResult)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    /**
     * Tìm kiếm channels
     */
    @GetMapping("/channels")
    @Operation(summary = "Tìm kiếm channels", description = "Tìm kiếm gần đúng theo tên channel")
    public ResponseEntity<List<SearchResponse.ChannelSearchResult>> searchChannels(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @Parameter(description = "ID server (optional)") @RequestParam(required = false) Long serverId) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Channel> channels;
        if (serverId != null) {
            channels = channelRepository.findByServerIdAndNameContainingIgnoreCase(serverId, keyword.trim());
        } else {
            channels = channelRepository.findByNameContainingIgnoreCase(keyword.trim());
        }

        List<SearchResponse.ChannelSearchResult> results = channels.stream()
                .map(this::toChannelSearchResult)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    /**
     * Tìm kiếm categories
     */
    @GetMapping("/categories")
    @Operation(summary = "Tìm kiếm categories", description = "Tìm kiếm gần đúng theo tên category")
    public ResponseEntity<List<SearchResponse.CategorySearchResult>> searchCategories(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @Parameter(description = "ID server (optional)") @RequestParam(required = false) Long serverId) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Category> categories;
        if (serverId != null) {
            categories = categoryRepository.findByServerIdAndNameContainingIgnoreCase(serverId, keyword.trim());
        } else {
            categories = categoryRepository.findByNameContainingIgnoreCase(keyword.trim());
        }

        List<SearchResponse.CategorySearchResult> results = categories.stream()
                .map(this::toCategorySearchResult)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    /**
     * Tìm kiếm members trong server
     */
    @GetMapping("/members")
    @Operation(summary = "Tìm kiếm members", description = "Tìm kiếm gần đúng theo displayName, userName hoặc nickname của member trong server")
    public ResponseEntity<List<SearchResponse.MemberSearchResult>> searchMembers(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @Parameter(description = "ID server (bắt buộc)") @RequestParam Long serverId) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (serverId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<SearchResponse.MemberSearchResult> results = serverMemberRepository
                .searchMembersInServer(serverId, keyword.trim())
                .stream()
                .map(this::toMemberSearchResult)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    // ==================== Helper mapping methods ====================

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
                .build();
    }
}
