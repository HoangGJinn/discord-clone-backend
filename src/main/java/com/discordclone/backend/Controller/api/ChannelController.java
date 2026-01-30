package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.CreateChannelRequest;
import com.discordclone.backend.dto.request.UpdateChannelRequest;
import com.discordclone.backend.dto.response.ChannelResponse;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.channel.ChannelService;
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
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class ChannelController {

    private final ChannelService channelService;

    // Tạo channel mới - chỉ OWNER hoặc ADMIN của server
    @PostMapping("/servers/{serverId}/channels")
    @PreAuthorize("@serverSecurity.isAdmin(#serverId, principal.id)")
    public ResponseEntity<ChannelResponse> createChannel(
            @PathVariable Long serverId,
            @Valid @RequestBody CreateChannelRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        request.setServerId(serverId);
        ChannelResponse response = channelService.createChannel(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // Lấy danh sách channels - chỉ member mới xem được
    @GetMapping("/servers/{serverId}/channels")
    @PreAuthorize("@serverSecurity.isMember(#serverId, principal.id)")
    public ResponseEntity<List<ChannelResponse>> getChannelsByServer(@PathVariable Long serverId) {
        List<ChannelResponse> channels = channelService.getChannelsByServer(serverId);
        return ResponseEntity.ok(channels);
    }

    // Lấy channels theo category - chỉ member mới xem được
    @GetMapping("/categories/{categoryId}/channels")
    @PreAuthorize("@serverSecurity.isMemberOfCategory(#categoryId, principal.id)")
    public ResponseEntity<List<ChannelResponse>> getChannelsByCategory(@PathVariable Long categoryId) {
        List<ChannelResponse> channels = channelService.getChannelsByCategory(categoryId);
        return ResponseEntity.ok(channels);
    }

    // Lấy thông tin channel - chỉ member mới xem được
    @GetMapping("/channels/{channelId}")
    @PreAuthorize("@serverSecurity.isMemberOfChannel(#channelId, principal.id)")
    public ResponseEntity<ChannelResponse> getChannelById(@PathVariable Long channelId) {
        ChannelResponse response = channelService.getChannelById(channelId);
        return ResponseEntity.ok(response);
    }

    // Cập nhật channel - chỉ OWNER hoặc ADMIN
    @PutMapping("/channels/{channelId}")
    @PreAuthorize("@serverSecurity.isAdminOfChannel(#channelId, principal.id)")
    public ResponseEntity<ChannelResponse> updateChannel(
            @PathVariable Long channelId,
            @Valid @RequestBody UpdateChannelRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ChannelResponse response = channelService.updateChannel(channelId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    // Xóa channel - chỉ OWNER hoặc ADMIN
    @DeleteMapping("/channels/{channelId}")
    @PreAuthorize("@serverSecurity.isAdminOfChannel(#channelId, principal.id)")
    public ResponseEntity<Map<String, String>> deleteChannel(
            @PathVariable Long channelId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        channelService.deleteChannel(channelId, userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Xóa channel thành công"));
    }
}
