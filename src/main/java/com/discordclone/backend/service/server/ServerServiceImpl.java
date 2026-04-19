package com.discordclone.backend.service.server;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discordclone.backend.dto.request.CreateServerRequest;
import com.discordclone.backend.dto.request.UpdateServerRequest;
import com.discordclone.backend.dto.response.*;
import com.discordclone.backend.entity.enums.MemberRole;
import com.discordclone.backend.entity.jpa.*;
import com.discordclone.backend.exception.ResourceNotFoundException;
import com.discordclone.backend.repository.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final CategoryRepository categoryRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    @Override
    public ServerResponse createServer(CreateServerRequest request, Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        // Tạo server mới
        Server server = Server.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .inviteCode(generateInviteCode())
                .owner(owner)
                .build();

        server = serverRepository.save(server);

        // Tự động thêm owner làm thành viên với role OWNER
        ServerMember ownerMember = ServerMember.builder()
                .user(owner)
                .server(server)
                .role(MemberRole.OWNER)
                .build();
        serverMemberRepository.save(ownerMember);

        // Tạo category mặc định để nhóm kênh text ban đầu
        Category defaultCategory = Category.builder()
            .name("TEXT CHANNELS")
            .position(0)
            .server(server)
            .build();
        defaultCategory = categoryRepository.save(defaultCategory);

        // Tạo channel mặc định "general" nằm trong category mặc định
        Channel generalChannel = Channel.builder()
                .name("general")
            .position(0)
                .server(server)
            .category(defaultCategory)
                .build();
        channelRepository.save(generalChannel);

        return mapToBasicResponse(server);
    }

    @Override
    @Transactional(readOnly = true)
    public ServerResponse getServerById(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server không tồn tại"));
        return mapToBasicResponse(server);
    }

    @Override
    @Transactional(readOnly = true)
    public ServerResponse getServerDetails(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server không tồn tại"));
        return mapToDetailResponse(server);
    }

    @Override
    public ServerResponse updateServer(Long serverId, UpdateServerRequest request, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server không tồn tại"));

        // Kiểm tra quyền (chỉ owner hoặc admin mới được sửa)
        checkAdminPermission(serverId, userId);

        if (request.getName() != null) {
            server.setName(request.getName());
        }
        if (request.getDescription() != null) {
            server.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            server.setIconUrl(request.getIconUrl());
        }

        server = serverRepository.save(server);
        return mapToBasicResponse(server);
    }

    @Override
    public void deleteServer(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server không tồn tại"));

        // Chỉ owner mới được xóa server
        if (!server.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể xóa server");
        }

        serverRepository.delete(server);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServerResponse> getUserServers(Long userId) {
        List<ServerMember> memberships = serverMemberRepository.findByUserId(userId);
        System.out.println("DEBUG: getUserServers - UserId: " + userId + ", Memberships found: " + memberships.size());
        return memberships.stream()
                .map(member -> mapToBasicResponse(member.getServer()))
                .collect(Collectors.toList());
    }

    @Override
    public ServerResponse joinServer(String inviteCode, Long userId) {
        Server server = serverRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mã mời không hợp lệ"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        // Kiểm tra đã là thành viên chưa
        if (serverMemberRepository.existsByServerIdAndUserId(server.getId(), userId)) {
            throw new RuntimeException("Bạn đã là thành viên của server này");
        }

        // Thêm thành viên mới
        ServerMember member = ServerMember.builder()
                .user(user)
                .server(server)
                .role(MemberRole.MEMBER)
                .build();
        serverMemberRepository.save(member);

        return mapToBasicResponse(server);
    }

    @Override
    public void leaveServer(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server không tồn tại"));

        // Owner không thể rời server
        if (server.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Chủ sở hữu không thể rời server. Hãy chuyển quyền sở hữu hoặc xóa server.");
        }

        serverMemberRepository.deleteByServerIdAndUserId(serverId, userId);
    }

    @Override
    public String regenerateInviteCode(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server không tồn tại"));

        checkAdminPermission(serverId, userId);

        String newCode = generateInviteCode();
        server.setInviteCode(newCode);
        serverRepository.save(server);

        return newCode;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServerMemberResponse> getServerMembers(Long serverId) {
        List<ServerMember> members = serverMemberRepository.findByServerId(serverId);
        return members.stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    // ============== Helper Methods ==============

    private String generateInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8);
        } while (serverRepository.existsByInviteCode(code));
        return code;
    }

    private void checkAdminPermission(Long serverId, Long userId) {
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của server này"));

        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này");
        }
    }

    private ServerResponse mapToBasicResponse(Server server) {
        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .inviteCode(server.getInviteCode())
                .ownerId(server.getOwner().getId())
                .ownerName(server.getOwner().getDisplayName())
                .memberCount((int) serverMemberRepository.countByServerId(server.getId()))
                .channelCount((int) channelRepository.countByServerId(server.getId()))
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }

    private ServerResponse mapToDetailResponse(Server server) {
        // Lấy categories và channels
        List<Category> categories = categoryRepository.findByServerIdOrderByPositionAsc(server.getId());
        List<Channel> standaloneChannels = channelRepository
                .findByServerIdAndCategoryIsNullOrderByPositionAsc(server.getId());
        List<ServerMember> members = serverMemberRepository.findByServerId(server.getId());

        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .inviteCode(server.getInviteCode())
                .ownerId(server.getOwner().getId())
                .ownerName(server.getOwner().getDisplayName())
                .memberCount(members.size())
                .channelCount((int) channelRepository.countByServerId(server.getId()))
                .categories(categories.stream().map(this::mapToCategoryResponse).collect(Collectors.toList()))
                .channels(standaloneChannels.stream().map(this::mapToChannelResponse).collect(Collectors.toList()))
                .members(members.stream().map(this::mapToMemberResponse).collect(Collectors.toList()))
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        List<Channel> channels = channelRepository.findByCategoryIdOrderByPositionAsc(category.getId());
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .position(category.getPosition())
                .serverId(category.getServer().getId())
                .channels(channels.stream().map(this::mapToChannelResponse).collect(Collectors.toList()))
                .createdAt(category.getCreatedAt())
                .build();
    }

    private ChannelResponse mapToChannelResponse(Channel channel) {
        return ChannelResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .type(channel.getType())
                .topic(channel.getTopic())
                .position(channel.getPosition())
                .serverId(channel.getServer().getId())
                .categoryId(channel.getCategory() != null ? channel.getCategory().getId() : null)
                .createdAt(channel.getCreatedAt())
                .build();
    }

    private ServerMemberResponse mapToMemberResponse(ServerMember member) {
        return ServerMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .userName(member.getUser().getUserName())
                .displayName(member.getUser().getDisplayName())
                .nickname(member.getNickname())
                .avatarUrl(member.getUser().getAvatarUrl())
                .status(member.getUser().getStatus())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .avatarEffectId(member.getUser().getAvatarEffectId())
                .bannerEffectId(member.getUser().getBannerEffectId())
                .cardEffectId(member.getUser().getCardEffectId())
                .build();
    }
}
