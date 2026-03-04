package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.response.FriendshipResponse;
import com.discordclone.backend.dto.response.UserSearchResponse;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.friend.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Friend API — quản lý bạn bè
 *
 * GET /api/users/search?keyword= — Tìm kiếm user
 * GET /api/friends — Danh sách bạn bè
 * GET /api/friends/requests/received — Lời mời nhận được (PENDING)
 * GET /api/friends/requests/sent — Lời mời đã gửi (PENDING)
 * POST /api/friends/request/{userId} — Gửi lời mời kết bạn
 * PUT /api/friends/{id}/accept — Chấp nhận lời mời
 * PUT /api/friends/{id}/reject — Từ chối lời mời
 * DELETE /api/friends/request/{id} — Hủy lời mời đã gửi
 * DELETE /api/friends/{id} — Xóa bạn bè
 * POST /api/friends/block/{userId} — Block user
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Friends", description = "API quản lý bạn bè")
public class FriendController {

    private final FriendService friendService;

    // ─── Tìm kiếm user ─────────────────────────────────────────────────────────

    @GetMapping("/api/users/search")
    @Operation(summary = "Tìm kiếm user theo username hoặc tên hiển thị")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String keyword,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<UserSearchResponse> results = friendService.searchUsers(keyword, userDetails.getId());
        return ResponseEntity.ok(results);
    }

    // ─── Danh sách bạn bè ──────────────────────────────────────────────────────

    @GetMapping("/api/friends")
    @Operation(summary = "Lấy danh sách bạn bè đã chấp nhận")
    public ResponseEntity<List<FriendshipResponse>> getFriends(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(friendService.getFriends(userDetails.getId()));
    }

    @GetMapping("/api/friends/requests/received")
    @Operation(summary = "Lấy danh sách lời mời kết bạn nhận được (PENDING)")
    public ResponseEntity<List<FriendshipResponse>> getPendingRequests(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(friendService.getPendingRequests(userDetails.getId()));
    }

    @GetMapping("/api/friends/requests/sent")
    @Operation(summary = "Lấy danh sách lời mời kết bạn đã gửi (PENDING)")
    public ResponseEntity<List<FriendshipResponse>> getSentRequests(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(friendService.getSentRequests(userDetails.getId()));
    }

    // ─── Gửi / hủy lời mời ─────────────────────────────────────────────────────

    @PostMapping("/api/friends/request/{receiverId}")
    @Operation(summary = "Gửi lời mời kết bạn")
    public ResponseEntity<FriendshipResponse> sendRequest(
            @PathVariable Long receiverId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        FriendshipResponse response = friendService.sendFriendRequest(userDetails.getId(), receiverId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/friends/request/{friendshipId}")
    @Operation(summary = "Hủy lời mời kết bạn đã gửi")
    public ResponseEntity<Map<String, String>> cancelRequest(
            @PathVariable Long friendshipId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        friendService.cancelFriendRequest(friendshipId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Đã hủy lời mời kết bạn"));
    }

    // ─── Chấp nhận / từ chối ───────────────────────────────────────────────────

    @PutMapping("/api/friends/{friendshipId}/accept")
    @Operation(summary = "Chấp nhận lời mời kết bạn")
    public ResponseEntity<FriendshipResponse> acceptRequest(
            @PathVariable Long friendshipId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        FriendshipResponse response = friendService.acceptFriendRequest(friendshipId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/friends/{friendshipId}/reject")
    @Operation(summary = "Từ chối lời mời kết bạn")
    public ResponseEntity<FriendshipResponse> rejectRequest(
            @PathVariable Long friendshipId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        FriendshipResponse response = friendService.rejectFriendRequest(friendshipId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // ─── Xóa bạn / Block ───────────────────────────────────────────────────────

    @DeleteMapping("/api/friends/{friendshipId}")
    @Operation(summary = "Xóa bạn bè")
    public ResponseEntity<Map<String, String>> unfriend(
            @PathVariable Long friendshipId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        friendService.unfriend(friendshipId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Đã xóa bạn bè"));
    }

    @PostMapping("/api/friends/block/{targetUserId}")
    @Operation(summary = "Block user")
    public ResponseEntity<FriendshipResponse> blockUser(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        FriendshipResponse response = friendService.blockUser(targetUserId, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
