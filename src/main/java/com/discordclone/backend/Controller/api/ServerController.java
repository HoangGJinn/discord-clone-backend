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
}
