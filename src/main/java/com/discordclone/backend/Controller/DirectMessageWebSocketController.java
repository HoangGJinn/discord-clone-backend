package com.discordclone.backend.Controller;

import com.discordclone.backend.dto.request.DirectMessageRequest;
import com.discordclone.backend.dto.response.DirectMessageResponse;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.service.directmessage.DirectMessageService;
import com.discordclone.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DirectMessageWebSocketController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    /**
     * Send message to user if they are online
     * Tries both username and email as principal names
     */
    private void sendToOnlinePrincipalNames(User target, Object payload) {
        List<String> names = new ArrayList<>();
        if (target.getUserName() != null) {
            names.add(target.getUserName());
        }
        if (target.getEmail() != null) {
            names.add(target.getEmail());
        }
        for (String name : names) {
            if (name != null && simpUserRegistry.getUser(name) != null) {
                messagingTemplate.convertAndSendToUser(name, "/queue/dm", payload);
            }
        }
    }

    /**
     * Handle direct message sending via WebSocket
     * Client sends to: /app/dm.send
     * Server broadcasts to: /user/{username}/queue/dm
     */
    @MessageMapping("/dm.send")
    public void sendDirectMessage(@Payload DirectMessageRequest request, Principal principal) {
        if (principal == null) {
            log.error("Unauthorized: No principal found for DM");
            return;
        }

        // Find sender by username
        var sender = userService.findByUserName(principal.getName()).orElse(null);
        if (sender == null) {
            log.error("Sender not found: {}", principal.getName());
            return;
        }

        Long senderId = sender.getId();
        DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

        // Send to receiver if online
        userService.findById(request.getReceiverId()).ifPresent(receiver -> {
            sendToOnlinePrincipalNames(receiver, response);
        });

        // Send back to sender for UI sync
        sendToOnlinePrincipalNames(sender, response);
    }
}