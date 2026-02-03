package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;
import com.discordclone.backend.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;

    // Client gửi tin nhắn tới: /app/chat/{channelId}
    @MessageMapping("/chat/{channelId}")
    // Server phát lại tin nhắn tới: /topic/channel/{channelId}
    @SendTo("/topic/channel/{channelId}")
    public ChatMessageResponse handleMessage(
            @DestinationVariable Long channelId,
            ChatMessageRequest messageReq) {

        // 1. Lưu tin nhắn vào Database
        ChatMessageResponse response = messageService.saveMessage(channelId, messageReq);

        // 2. Trả về response (Spring sẽ tự động gửi tới @SendTo)
        return response;
    }
}
