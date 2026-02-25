package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.response.SearchResponse;
import com.discordclone.backend.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "Search", description = "APIs tìm kiếm gần đúng (fuzzy search)")
public class SearchController {

    private final SearchService searchService;

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
}
