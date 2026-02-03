package com.discordclone.backend.Controller.api;

import com.discordclone.backend.dto.response.ChatMessageResponse;
import com.discordclone.backend.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
