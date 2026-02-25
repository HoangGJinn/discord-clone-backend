package com.discordclone.backend.service.category;

import com.discordclone.backend.dto.request.CreateCategoryRequest;
import com.discordclone.backend.dto.request.UpdateCategoryRequest;
import com.discordclone.backend.dto.response.CategoryResponse;
import com.discordclone.backend.dto.response.ChannelResponse;
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
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request, Long userId) {
        Server server = serverRepository.findById(request.getServerId())
                .orElseThrow(() -> new ResourceNotFoundException("Server không tồn tại"));

        // Kiểm tra quyền
        checkAdminPermission(server.getId(), userId);

        // Tính position mới
        int newPosition = (int) categoryRepository.countByServerId(server.getId());

        Category category = Category.builder()
                .name(request.getName())
                .server(server)
                .position(newPosition)
                .build();

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));
        return mapToResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));

        // Kiểm tra quyền
        checkAdminPermission(category.getServer().getId(), userId);

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getPosition() != null) {
            category.setPosition(request.getPosition());
        }

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));

        // Kiểm tra quyền
        checkAdminPermission(category.getServer().getId(), userId);

        // Xóa category (channels trong category sẽ bị set category = null)
        List<Channel> channels = channelRepository.findByCategoryIdOrderByPositionAsc(categoryId);
        for (Channel channel : channels) {
            channel.setCategory(null);
            channelRepository.save(channel);
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByServer(Long serverId) {
        List<Category> categories = categoryRepository.findByServerIdOrderByPositionAsc(serverId);
        return categories.stream()
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

    private CategoryResponse mapToResponse(Category category) {
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
}
