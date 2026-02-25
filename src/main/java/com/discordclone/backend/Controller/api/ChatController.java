package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

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
            ChatMessageRequest messageReq,
            Principal principal) {

        // Lấy senderId từ authenticated Principal do WebSocketAuthChannelInterceptor
        // set
        if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
            if (authToken.getPrincipal() instanceof UserDetailsImpl userDetails) {
                messageReq.setSenderId(userDetails.getId());
            }
        }

        return messageService.saveMessage(channelId, messageReq);
    }
}
