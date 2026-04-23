package com.discordclone.backend.service.server;

import com.discordclone.backend.dto.request.CreateServerRequest;
import com.discordclone.backend.dto.request.UpdateServerRequest;
import com.discordclone.backend.dto.response.ServerMemberResponse;
import com.discordclone.backend.dto.response.ServerResponse;

import java.util.List;

public interface ServerService {

    // Tạo server mới
    ServerResponse createServer(CreateServerRequest request, Long userId);

    // Lấy thông tin server theo ID
    ServerResponse getServerById(Long serverId);

    // Lấy thông tin server đầy đủ (bao gồm categories, channels, members)
    ServerResponse getServerDetails(Long serverId, Long userId);

    // Cập nhật server
    ServerResponse updateServer(Long serverId, UpdateServerRequest request, Long userId);

    // Xóa server
    void deleteServer(Long serverId, Long userId);

    // Lấy danh sách servers mà user là thành viên
    List<ServerResponse> getUserServers(Long userId);

    // Join server bằng invite code
    ServerResponse joinServer(String inviteCode, Long userId);

    // Rời server
    void leaveServer(Long serverId, Long userId);

    // Tạo invite code mới
    String regenerateInviteCode(Long serverId, Long userId);

    // Lấy danh sách thành viên của server
    List<ServerMemberResponse> getServerMembers(Long serverId);

    // Chuyển quyền sở hữu server
    void transferOwnership(Long serverId, Long currentOwnerId, Long newOwnerId);

    // Kick thành viên (xóa khỏi server)
    void kickMember(Long serverId, Long targetUserId, Long requesterId);

    // Ban thành viên (đánh dấu banned, xóa khỏi server)
    void banMember(Long serverId, Long targetUserId, Long requesterId);

    // Timeout thành viên trong X phút (cấm chat)
    void timeoutMember(Long serverId, Long targetUserId, Long requesterId, int minutes);

    // Gỡ timeout thành viên
    void removeTimeout(Long serverId, Long targetUserId, Long requesterId);

    // Cập nhật vai trò thành viên
    void updateMemberRole(Long serverId, Long targetUserId, Long requesterId, com.discordclone.backend.entity.enums.MemberRole newRole);
}

