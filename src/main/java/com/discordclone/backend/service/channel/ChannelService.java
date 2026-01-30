package com.discordclone.backend.service.channel;

import com.discordclone.backend.dto.request.CreateChannelRequest;
import com.discordclone.backend.dto.request.UpdateChannelRequest;
import com.discordclone.backend.dto.response.ChannelResponse;

import java.util.List;

public interface ChannelService {

    // Tạo channel mới
    ChannelResponse createChannel(CreateChannelRequest request, Long userId);

    // Lấy thông tin channel theo ID
    ChannelResponse getChannelById(Long channelId);

    // Cập nhật channel
    ChannelResponse updateChannel(Long channelId, UpdateChannelRequest request, Long userId);

    // Xóa channel
    void deleteChannel(Long channelId, Long userId);

    // Lấy danh sách channels của server
    List<ChannelResponse> getChannelsByServer(Long serverId);

    // Lấy danh sách channels của category
    List<ChannelResponse> getChannelsByCategory(Long categoryId);
}
