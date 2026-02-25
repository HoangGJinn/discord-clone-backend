package com.discordclone.backend.service.message;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;
import com.discordclone.backend.dto.response.SocketResponse;
import com.discordclone.backend.entity.mongo.ChannelMessage;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.ChannelRepository;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.repository.mongo.ChannelMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

        private final ChannelMessageRepository messageRepository;
        private final UserRepository userRepository;
        private final ChannelRepository channelRepository;
        private final SimpMessagingTemplate messagingTemplate;

        @Override
        public ChatMessageResponse saveMessage(Long channelId, ChatMessageRequest req) {
                // Verify channel exists
                channelRepository.findById(channelId)
                                .orElseThrow(() -> new RuntimeException("Channel not found"));

                // Verify sender exists
                User sender = userRepository.findById(req.getSenderId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Date now = new Date();

                ChannelMessage message = new ChannelMessage();
                message.setChannelId(channelId);
                message.setSenderId(sender.getId());
                message.setSenderName(sender.getDisplayName());
                message.setSenderAvatar(sender.getAvatarUrl());
                message.setContent(req.getContent());
                message.setAttachments(req.getAttachments() != null ? req.getAttachments() : new ArrayList<>());
                message.setEdited(false);
                message.setDeleted(false);
                message.setPinned(false);
                message.setCreatedAt(now);
                message.setUpdatedAt(now);

                ChannelMessage saved = messageRepository.save(message);
                return mapToResponse(saved);
        }

        @Override
        public List<ChatMessageResponse> getMessagesByChannel(Long channelId) {
                List<ChannelMessage> messages = messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);
                return messages.stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public void deleteMessage(String messageId, Long userId) {
                ChannelMessage message = messageRepository.findById(messageId)
                                .orElseThrow(() -> new RuntimeException("Message not found"));

                if (!message.getSenderId().equals(userId)) {
                        throw new RuntimeException("You are not allowed to delete this message");
                }

                message.setDeleted(true);
                message.setContent("Tin nhắn đã bị xóa");
                message.setUpdatedAt(new Date());
                messageRepository.save(message);

                SocketResponse socketResponse = SocketResponse.builder()
                                .type("DELETE")
                                .data(messageId)
                                .build();
                messagingTemplate.convertAndSend("/topic/channel/" + message.getChannelId(), socketResponse);
        }

        @Override
        public ChatMessageResponse editMessage(String messageId, Long userId, String newContent) {
                ChannelMessage message = messageRepository.findById(messageId)
                                .orElseThrow(() -> new RuntimeException("Message not found"));

                if (!message.getSenderId().equals(userId)) {
                        throw new RuntimeException("You are not allowed to edit this message");
                }

                message.setContent(newContent);
                message.setEdited(true);
                message.setUpdatedAt(new Date());
                ChannelMessage updated = messageRepository.save(message);

                ChatMessageResponse response = mapToResponse(updated);

                SocketResponse socketResponse = SocketResponse.builder()
                                .type("EDIT")
                                .data(response)
                                .build();
                messagingTemplate.convertAndSend("/topic/channel/" + message.getChannelId(), socketResponse);

                return response;
        }

        private ChatMessageResponse mapToResponse(ChannelMessage msg) {
                return ChatMessageResponse.builder()
                                .id(msg.getId())
                                .channelId(msg.getChannelId())
                                .senderId(msg.getSenderId())
                                .senderName(msg.getSenderName())
                                .senderAvatar(msg.getSenderAvatar())
                                .content(msg.getContent())
                                .attachments(msg.getAttachments())
                                .edited(msg.getEdited())
                                .deleted(msg.getDeleted())
                                .pinned(msg.getPinned())
                                .createdAt(msg.getCreatedAt())
                                .updatedAt(msg.getUpdatedAt())
                                .reactions(msg.getReactions())
                                .build();
        }
}
