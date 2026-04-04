package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;
import com.discordclone.backend.service.message.MessageService;
import com.discordclone.backend.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class MessageController {

    private final MessageService messageService;

    // Lấy lịch sử tin nhắn của channel - chỉ member mới xem được
    @GetMapping("/channels/{channelId}/messages")
    @PreAuthorize("@serverSecurity.isMemberOfChannel(#channelId, principal.id)")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesByChannel(@PathVariable Long channelId) {
        List<ChatMessageResponse> messages = messageService.getMessagesByChannel(channelId);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        messageService.deleteMessage(messageId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessageResponse> editMessage(
            @PathVariable String messageId,
            @RequestBody ChatMessageRequest req,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        ChatMessageResponse response = messageService.editMessage(messageId, userDetails.getId(), req.getContent());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<Void> addReaction(
            @PathVariable String messageId,
            @RequestParam String emoji,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        messageService.addReaction(messageId, userDetails.getId(), emoji);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/messages/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @PathVariable String messageId,
            @RequestParam String emoji,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        messageService.removeReaction(messageId, userDetails.getId(), emoji);
        return ResponseEntity.ok().build();
    }
}
