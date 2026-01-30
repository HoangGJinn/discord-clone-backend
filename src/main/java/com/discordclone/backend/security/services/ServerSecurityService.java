package com.discordclone.backend.security.services;

import com.discordclone.backend.entity.enums.MemberRole;
import com.discordclone.backend.entity.jpa.Category;
import com.discordclone.backend.entity.jpa.Channel;
import com.discordclone.backend.entity.jpa.ServerMember;
import com.discordclone.backend.repository.CategoryRepository;
import com.discordclone.backend.repository.ChannelRepository;
import com.discordclone.backend.repository.ServerMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Custom security service để kiểm tra quyền trong server
 * Sử dụng với @PreAuthorize("@serverSecurity.isOwner(#serverId, principal.id)")
 */
@Service("serverSecurity")
@RequiredArgsConstructor
public class ServerSecurityService {

    private final ServerMemberRepository serverMemberRepository;
    private final CategoryRepository categoryRepository;
    private final ChannelRepository channelRepository;

    /**
     * Kiểm tra user có phải OWNER của server không
     */
    public boolean isOwner(Long serverId, Long userId) {
        Optional<ServerMember> member = serverMemberRepository.findByServerIdAndUserId(serverId, userId);
        return member.isPresent() && member.get().getRole() == MemberRole.OWNER;
    }

    /**
     * Kiểm tra user có phải OWNER hoặc ADMIN của server không
     */
    public boolean isAdmin(Long serverId, Long userId) {
        Optional<ServerMember> member = serverMemberRepository.findByServerIdAndUserId(serverId, userId);
        if (member.isEmpty())
            return false;
        MemberRole role = member.get().getRole();
        return role == MemberRole.OWNER || role == MemberRole.ADMIN;
    }

    /**
     * Kiểm tra user có phải thành viên của server không
     */
    public boolean isMember(Long serverId, Long userId) {
        return serverMemberRepository.existsByServerIdAndUserId(serverId, userId);
    }

    /**
     * Kiểm tra user có quyền admin trong server chứa category không
     */
    public boolean isAdminOfCategory(Long categoryId, Long userId) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (category.isEmpty())
            return false;
        return isAdmin(category.get().getServer().getId(), userId);
    }

    /**
     * Kiểm tra user có quyền admin trong server chứa channel không
     */
    public boolean isAdminOfChannel(Long channelId, Long userId) {
        Optional<Channel> channel = channelRepository.findById(channelId);
        if (channel.isEmpty())
            return false;
        return isAdmin(channel.get().getServer().getId(), userId);
    }

    /**
     * Kiểm tra user là member trong server chứa category không
     */
    public boolean isMemberOfCategory(Long categoryId, Long userId) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (category.isEmpty())
            return false;
        return isMember(category.get().getServer().getId(), userId);
    }

    /**
     * Kiểm tra user là member trong server chứa channel không
     */
    public boolean isMemberOfChannel(Long channelId, Long userId) {
        Optional<Channel> channel = channelRepository.findById(channelId);
        if (channel.isEmpty())
            return false;
        return isMember(channel.get().getServer().getId(), userId);
    }
}
