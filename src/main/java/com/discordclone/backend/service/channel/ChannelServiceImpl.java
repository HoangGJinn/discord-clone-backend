package com.discordclone.backend.service.channel;

import com.discordclone.backend.dto.request.CreateChannelRequest;
import com.discordclone.backend.dto.request.UpdateChannelRequest;
import com.discordclone.backend.dto.response.ChannelResponse;
import com.discordclone.backend.entity.enums.ChannelType;
import com.discordclone.backend.entity.enums.MemberRole;
import com.discordclone.backend.entity.jpa.Category;
import com.discordclone.backend.entity.jpa.Channel;
import com.discordclone.backend.entity.jpa.Server;
import com.discordclone.backend.entity.jpa.ServerMember;
import com.discordclone.backend.exception.ResourceNotFoundException;
import com.discordclone.backend.repository.CategoryRepository;
import com.discordclone.backend.repository.ChannelRepository;
import com.discordclone.backend.repository.ServerMemberRepository;
import com.discordclone.backend.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;

    @Override
    public ChannelResponse createChannel(CreateChannelRequest request, Long userId) {
        Server server = serverRepository.findById(request.getServerId())
                .orElseThrow(() -> new ResourceNotFoundException("Server không tồn tại"));

        // Kiểm tra quyền
        checkAdminPermission(server.getId(), userId);

        // Lấy category nếu có
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));

            // Kiểm tra category thuộc server
            if (!category.getServer().getId().equals(server.getId())) {
                throw new RuntimeException("Category không thuộc server này");
            }
        }

        // Tính position mới
        int newPosition;
        if (category != null) {
            newPosition = (int) channelRepository.countByCategoryId(category.getId());
        } else {
            newPosition = (int) channelRepository.countByServerId(server.getId());
        }

        Channel channel = Channel.builder()
                .name(request.getName())
                .type(request.getType())
                .topic(request.getTopic())
                .type(request.getType() != null ? request.getType() : ChannelType.TEXT)
                .bitrate(request.getBitrate() != null ? request.getBitrate() : 64000)
                .userLimit(request.getUserLimit() != null ? request.getUserLimit() : 0)
                .server(server)
                .category(category)
                .position(newPosition)
                .build();

        channel = channelRepository.save(channel);
        return mapToResponse(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelResponse getChannelById(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel không tồn tại"));
        return mapToResponse(channel);
    }

    @Override
    public ChannelResponse updateChannel(Long channelId, UpdateChannelRequest request, Long userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel không tồn tại"));

        // Kiểm tra quyền
        checkAdminPermission(channel.getServer().getId(), userId);

        if (request.getName() != null) {
            channel.setName(request.getName());
        }
        if (request.getTopic() != null) {
            channel.setTopic(request.getTopic());
        }
        if (request.getBitrate() != null) {
            channel.setBitrate(request.getBitrate());
        }
        if (request.getUserLimit() != null) {
            channel.setUserLimit(request.getUserLimit());
        }
        if (request.getPosition() != null) {
            channel.setPosition(request.getPosition());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));

            // Kiểm tra category thuộc cùng server
            if (!category.getServer().getId().equals(channel.getServer().getId())) {
                throw new RuntimeException("Category không thuộc server này");
            }
            channel.setCategory(category);
        }

        channel = channelRepository.save(channel);
        return mapToResponse(channel);
    }

    @Override
    public void deleteChannel(Long channelId, Long userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel không tồn tại"));

        // Kiểm tra quyền
        checkAdminPermission(channel.getServer().getId(), userId);

        channelRepository.delete(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> getChannelsByServer(Long serverId) {
        List<Channel> channels = channelRepository.findByServerIdOrderByPositionAsc(serverId);
        return channels.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> getChannelsByCategory(Long categoryId) {
        List<Channel> channels = channelRepository.findByCategoryIdOrderByPositionAsc(categoryId);
        return channels.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============== Helper Methods ==============

    private void checkAdminPermission(Long serverId, Long userId) {
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của server này"));

        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này");
        }
    }

    private ChannelResponse mapToResponse(Channel channel) {
        return ChannelResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .type(channel.getType())
                .topic(channel.getTopic())
                .position(channel.getPosition())
                .bitrate(channel.getBitrate())
                .userLimit(channel.getUserLimit())
                .serverId(channel.getServer().getId())
                .categoryId(channel.getCategory() != null ? channel.getCategory().getId() : null)
                .createdAt(channel.getCreatedAt())
                .build();
    }
}
