package com.discordclone.backend.service.message;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;

public interface MessageService {
    ChatMessageResponse saveMessage(Long channelId, ChatMessageRequest req);

    java.util.List<ChatMessageResponse> getMessagesByChannel(Long channelId);

}
