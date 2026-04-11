package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.DirectMessageRequest;
import com.discordclone.backend.dto.request.EditMessageRequest;
import com.discordclone.backend.dto.response.ConversationResponse;
import com.discordclone.backend.dto.response.DirectMessageResponse;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.directmessage.DirectMessageService;
import com.discordclone.backend.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/direct-messages")
@Tag(name = "Direct Messages", description = "Operations for private 1-on-1 messaging")
@RequiredArgsConstructor
@Slf4j
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    private void sendSocketToUser(Long userId, Object payload) {
        userService.findById(userId).ifPresent(user -> {
            if (user.getUserName() != null) {
                messagingTemplate.convertAndSendToUser(user.getUserName(), "/queue/dm", payload);
            }
        });
    }

    @Operation(summary = "Get all conversations for current user")
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        return ResponseEntity.ok(directMessageService.getConversationList(user.getId()));
    }

    @Operation(summary = "Send Direct Message")
    @PostMapping
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody DirectMessageRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        Long senderId = user.getId();
        DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

        // Broadcast to conversation topic (reliable — works like chat channel)
        messagingTemplate.convertAndSend("/topic/dm/" + response.getConversationId(), response);

        // Also keep user-queue push as fallback
        sendSocketToUser(request.getReceiverId(), response);
        sendSocketToUser(senderId, response);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Conversation Messages")
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<DirectMessageResponse>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                directMessageService.getMessages(conversationId, user.getId(), pageable));
    }

    @Operation(summary = "Init Conversation with another user")
    @PostMapping("/conversation/init")
    public ResponseEntity<ConversationResponse> initConversation(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long receiverId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        return ResponseEntity.ok(
                directMessageService.getOrCreateConversation(user.getId(), receiverId));
    }

    @Operation(summary = "Get or create DM conversation by friend userId")
    @GetMapping("/conversation/by-user/{friendId}")
    public ResponseEntity<ConversationResponse> getOrCreateConversationByUser(
            @PathVariable Long friendId,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        ConversationResponse conv = directMessageService.getOrCreateConversation(user.getId(), friendId);
        return ResponseEntity.ok(conv);
    }

    @Operation(summary = "Edit Direct Message")
    @PutMapping("/{messageId}")
    public ResponseEntity<DirectMessageResponse> editMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request) {
        DirectMessageResponse response = directMessageService.editMessage(
                messageId, userDetails.getId(), request);

        // Notify both users via WebSocket
        sendSocketToUser(response.getReceiverId(), response);
        sendSocketToUser(userDetails.getId(), response);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Direct Message")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String messageId) {
        DirectMessageResponse response = directMessageService.deleteMessage(
                messageId, userDetails.getId());

        // Notify both users via WebSocket
        sendSocketToUser(response.getReceiverId(), response);
        sendSocketToUser(userDetails.getId(), response);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add reaction to a message")
    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<Void> addReaction(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String messageId,
            @RequestParam String emoji) {
        directMessageService.addReaction(messageId, userDetails.getId(), emoji);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove reaction from a message")
    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String messageId) {
        directMessageService.removeReaction(messageId, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
