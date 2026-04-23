package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.response.MessageSearchResult;
import com.discordclone.backend.dto.response.SearchResponse;
import com.discordclone.backend.entity.mongo.ChannelMessage;
import com.discordclone.backend.entity.mongo.DirectMessage;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.repository.mongo.ChannelMessageRepository;
import com.discordclone.backend.repository.mongo.DirectMessageRepository;
import com.discordclone.backend.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "Search", description = "APIs tìm kiếm gần đúng (fuzzy search)")
public class SearchController {

    private final SearchService searchService;
    private final DirectMessageRepository directMessageRepository;
    private final ChannelMessageRepository channelMessageRepository;
    private final UserRepository userRepository;

    /**
     * Tìm kiếm tổng hợp - tìm trong tất cả entities
     */
    @GetMapping
    @Operation(summary = "Tìm kiếm tổng hợp", description = "Tìm kiếm gần đúng trong servers, channels, categories. Sử dụng LIKE %keyword%")
    public ResponseEntity<SearchResponse> searchAll(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @Parameter(description = "ID server (optional - để lọc channels, categories)") @RequestParam(required = false) Long serverId) {

        try {
            SearchResponse response = searchService.searchAll(keyword, serverId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tìm kiếm servers
     */
    @GetMapping("/servers")
    @Operation(summary = "Tìm kiếm servers", description = "Tìm kiếm gần đúng theo tên hoặc mô tả server")
    public ResponseEntity<List<SearchResponse.ServerSearchResult>> searchServers(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword) {

        try {
            List<SearchResponse.ServerSearchResult> results = searchService.searchServers(keyword);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tìm kiếm channels
     */
    @GetMapping("/channels")
    @Operation(summary = "Tìm kiếm channels", description = "Tìm kiếm gần đúng theo tên channel")
    public ResponseEntity<List<SearchResponse.ChannelSearchResult>> searchChannels(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @Parameter(description = "ID server (optional)") @RequestParam(required = false) Long serverId) {

        try {
            List<SearchResponse.ChannelSearchResult> results = searchService.searchChannels(keyword, serverId);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tìm kiếm categories
     */
    @GetMapping("/categories")
    @Operation(summary = "Tìm kiếm categories", description = "Tìm kiếm gần đúng theo tên category")
    public ResponseEntity<List<SearchResponse.CategorySearchResult>> searchCategories(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @Parameter(description = "ID server (optional)") @RequestParam(required = false) Long serverId) {

        try {
            List<SearchResponse.CategorySearchResult> results = searchService.searchCategories(keyword, serverId);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tìm kiếm members trong server
     */
    @GetMapping("/members")
    @Operation(summary = "Tìm kiếm members", description = "Tìm kiếm gần đúng theo displayName, userName hoặc nickname của member trong server")
    public ResponseEntity<List<SearchResponse.MemberSearchResult>> searchMembers(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @Parameter(description = "ID server (bắt buộc)") @RequestParam Long serverId) {

        try {
            List<SearchResponse.MemberSearchResult> results = searchService.searchMembers(keyword, serverId);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Search messages in a DM conversation")
    @GetMapping("/dm-messages")
    public ResponseEntity<List<MessageSearchResult>> searchDmMessages(
            @Parameter(description = "Conversation ID") @RequestParam String conversationId,
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (keyword == null || keyword.trim().length() < 1) {
            return ResponseEntity.badRequest().build();
        }

        Page<DirectMessage> results = directMessageRepository
                .findByConversationIdAndContentContainingIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(
                        conversationId, keyword.trim(), PageRequest.of(page, size));

        List<MessageSearchResult> dto = results.getContent().stream().map(msg -> {
            String senderName = "Unknown";
            String senderAvatar = null;
            String senderAvatarEffectId = null;
            if (msg.getSenderId() != null) {
                var userOpt = userRepository.findById(msg.getSenderId());
                if (userOpt.isPresent()) {
                    var u = userOpt.get();
                    senderName = u.getDisplayName() != null ? u.getDisplayName() : u.getUserName();
                    senderAvatar = u.getAvatarUrl();
                    senderAvatarEffectId = u.getAvatarEffectId();
                }
            }
            return MessageSearchResult.builder()
                    .id(msg.getId())
                    .content(msg.getContent())
                    .senderName(senderName)
                    .senderAvatar(senderAvatar)
                    .senderAvatarEffectId(senderAvatarEffectId)
                    .createdAt(msg.getCreatedAt())
                    .conversationId(msg.getConversationId())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Search messages in a Channel")
    @GetMapping("/channel-messages")
    public ResponseEntity<List<MessageSearchResult>> searchChannelMessages(
            @Parameter(description = "Channel ID") @RequestParam Long channelId,
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (keyword == null || keyword.trim().length() < 1) {
            return ResponseEntity.badRequest().build();
        }

        Page<ChannelMessage> results = channelMessageRepository
                .findByChannelIdAndContentContainingIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(
                        channelId, keyword.trim(), PageRequest.of(page, size));

        List<MessageSearchResult> dto = results.getContent().stream().map(msg -> {
            String senderAvatarEffectId = msg.getSenderAvatarEffectId();
            return MessageSearchResult.builder()
                    .id(msg.getId())
                    .content(msg.getContent())
                    .senderName(msg.getSenderName() != null ? msg.getSenderName() : "Unknown")
                    .senderAvatar(msg.getSenderAvatar())
                    .senderAvatarEffectId(senderAvatarEffectId)
                    .createdAt(msg.getCreatedAt())
                    .channelId(msg.getChannelId())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }
}
