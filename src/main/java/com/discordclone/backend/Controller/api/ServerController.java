package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.CreateServerRequest;
import com.discordclone.backend.dto.request.JoinServerRequest;
import com.discordclone.backend.dto.request.UpdateServerRequest;
import com.discordclone.backend.dto.response.ServerMemberResponse;
import com.discordclone.backend.dto.response.ServerResponse;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.server.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/servers")
@PreAuthorize("isAuthenticated()")
public class ServerController {

    private final ServerService serverService;

    // Tạo server mới
    @PostMapping
    public ResponseEntity<ServerResponse> createServer(
            @Valid @RequestBody CreateServerRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ServerResponse response = serverService.createServer(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // Lấy danh sách servers của user
    @GetMapping("/my-servers")
    public ResponseEntity<List<ServerResponse>> getMyServers(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ServerResponse> servers = serverService.getUserServers(userDetails.getId());
        return ResponseEntity.ok(servers);
    }

    // Lấy thông tin server (cơ bản) - chỉ member mới xem được
    @GetMapping("/{serverId}")
    @PreAuthorize("@serverSecurity.isMember(#serverId, principal.id)")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable Long serverId) {
        ServerResponse response = serverService.getServerById(serverId);
        return ResponseEntity.ok(response);
    }

    // Lấy thông tin chi tiết server - chỉ member mới xem được
    @GetMapping("/{serverId}/details")
    @PreAuthorize("@serverSecurity.isMember(#serverId, principal.id)")
    public ResponseEntity<ServerResponse> getServerDetails(
            @PathVariable Long serverId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ServerResponse response = serverService.getServerDetails(serverId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // Cập nhật server - chỉ OWNER hoặc ADMIN
    @PutMapping("/{serverId}")
    @PreAuthorize("@serverSecurity.isAdmin(#serverId, principal.id)")
    public ResponseEntity<ServerResponse> updateServer(
            @PathVariable Long serverId,
            @Valid @RequestBody UpdateServerRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ServerResponse response = serverService.updateServer(serverId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // Xóa server - chỉ OWNER mới được xóa
    @DeleteMapping("/{serverId}")
    @PreAuthorize("@serverSecurity.isOwner(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> deleteServer(
            @PathVariable Long serverId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        serverService.deleteServer(serverId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Xóa server thành công"));
    }

    // Join server bằng invite code
    @PostMapping("/join")
    public ResponseEntity<ServerResponse> joinServer(
            @Valid @RequestBody JoinServerRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ServerResponse response = serverService.joinServer(request.getInviteCode(), userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // Rời server - phải là member mới rời được
    @PostMapping("/{serverId}/leave")
    @PreAuthorize("@serverSecurity.isMember(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> leaveServer(
            @PathVariable Long serverId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        serverService.leaveServer(serverId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Đã rời server thành công"));
    }

    // Tạo invite code mới - chỉ OWNER hoặc ADMIN
    @PostMapping("/{serverId}/invite-code")
    @PreAuthorize("@serverSecurity.isAdmin(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> regenerateInviteCode(
            @PathVariable Long serverId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String newCode = serverService.regenerateInviteCode(serverId, userDetails.getId());
        return ResponseEntity.ok(Map.of("inviteCode", newCode));
    }

    // Lấy danh sách thành viên - chỉ member mới xem được
    @GetMapping("/{serverId}/members")
    @PreAuthorize("@serverSecurity.isMember(#serverId, principal.id)")
    public ResponseEntity<List<ServerMemberResponse>> getServerMembers(@PathVariable Long serverId) {
        List<ServerMemberResponse> members = serverService.getServerMembers(serverId);
        return ResponseEntity.ok(members);
    }

    // Chuyển quyền sở hữu server - chỉ OWNER mới được làm
    @PostMapping("/{serverId}/transfer-ownership")
    @PreAuthorize("@serverSecurity.isOwner(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> transferOwnership(
            @PathVariable Long serverId,
            @RequestParam Long newOwnerId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        serverService.transferOwnership(serverId, userDetails.getId(), newOwnerId);
        return ResponseEntity.ok(Map.of("message", "Đã chuyển quyền sở hữu server thành công"));
    }

    // Kick thành viên - chỉ OWNER/ADMIN
    @DeleteMapping("/{serverId}/members/{targetUserId}/kick")
    @PreAuthorize("@serverSecurity.isAdmin(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> kickMember(
            @PathVariable Long serverId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        serverService.kickMember(serverId, targetUserId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Đã kick thành viên"));
    }

    // Ban thành viên - chỉ OWNER/ADMIN
    @PostMapping("/{serverId}/members/{targetUserId}/ban")
    @PreAuthorize("@serverSecurity.isAdmin(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> banMember(
            @PathVariable Long serverId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        serverService.banMember(serverId, targetUserId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Đã ban thành viên"));
    }

    // Timeout thành viên - chỉ OWNER/ADMIN
    @PostMapping("/{serverId}/members/{targetUserId}/timeout")
    @PreAuthorize("@serverSecurity.isAdmin(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> timeoutMember(
            @PathVariable Long serverId,
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "10") int minutes,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        serverService.timeoutMember(serverId, targetUserId, userDetails.getId(), minutes);
        return ResponseEntity.ok(Map.of("message", "Đã timeout thành viên trong " + minutes + " phút"));
    }

    // Gỡ timeout thành viên - chỉ OWNER/ADMIN
    @PostMapping("/{serverId}/members/{targetUserId}/remove-timeout")
    @PreAuthorize("@serverSecurity.isAdmin(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> removeTimeout(
            @PathVariable Long serverId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        serverService.removeTimeout(serverId, targetUserId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Đã gỡ timeout thành viên"));
    }

    // Cập nhật vai trò thành viên - chỉ OWNER
    @PutMapping("/{serverId}/members/{targetUserId}/role")
    @PreAuthorize("@serverSecurity.isOwner(#serverId, principal.id)")
    public ResponseEntity<Map<String, String>> updateMemberRole(
            @PathVariable Long serverId,
            @PathVariable Long targetUserId,
            @RequestParam com.discordclone.backend.entity.enums.MemberRole role,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        serverService.updateMemberRole(serverId, targetUserId, userDetails.getId(), role);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật vai trò thành viên thành công"));
    }
}

