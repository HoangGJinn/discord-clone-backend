package com.discordclone.backend.service.directmessage;

import com.discordclone.backend.dto.request.DirectMessageRequest;
import com.discordclone.backend.dto.request.EditMessageRequest;
import com.discordclone.backend.dto.response.ConversationResponse;
import com.discordclone.backend.dto.response.DirectMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface DirectMessageService {

    DirectMessageResponse sendMessage(Long senderId, DirectMessageRequest request);

    Page<DirectMessageResponse> getMessages(String conversationId, Long userId, Pageable pageable);

    ConversationResponse getOrCreateConversation(Long userId1, Long userId2);

    List<ConversationResponse> getConversationList(Long userId);

    DirectMessageResponse editMessage(String messageId, Long userId, EditMessageRequest request);

    DirectMessageResponse deleteMessage(String messageId, Long userId);

    void addReaction(String messageId, Long userId, String emoji);

    void removeReaction(String messageId, Long userId);
}
