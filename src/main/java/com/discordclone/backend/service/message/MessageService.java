package com.discordclone.backend.service.message;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;

import java.util.List;

public interface MessageService {
    ChatMessageResponse saveMessage(Long channelId, ChatMessageRequest req);

    List<ChatMessageResponse> getMessagesByChannel(Long channelId);

    void deleteMessage(String messageId, Long userId);

    ChatMessageResponse editMessage(String messageId, Long userId, String newContent);

    void addReaction(String messageId, Long userId, String emoji);

    void removeReaction(String messageId, Long userId, String emoji);

    void markChannelAsRead(Long channelId, Long userId);

    void markChannelAsUnread(Long channelId, Long userId);
}
