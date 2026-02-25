package com.discordclone.backend.service.message;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;
import com.discordclone.backend.dto.response.SocketResponse;
import com.discordclone.backend.entity.mongo.ChannelMessage;
import com.discordclone.backend.entity.jpa.Channel;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.ChannelRepository;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.repository.mongo.ChannelMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
                // Verify channel exists (MySQL)
                Channel channel = channelRepository.findById(channelId)
                                .orElseThrow(() -> new RuntimeException("Channel not found"));

                // Verify sender exists (MySQL)
                User sender = userRepository.findById(req.getSenderId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                ChannelMessage message = ChannelMessage.builder()
                                .channelId(channelId)
                                .userId(sender.getId())
                                .content(req.getContent())
                                .createdAt(new Date())
                                .updatedAt(new Date())
                                .edited(false)
                                .deleted(false)
                                .build();

                ChannelMessage saved = messageRepository.save(message);

                return mapToResponse(saved, sender);
        }

        @Override
        public List<ChatMessageResponse> getMessagesByChannel(Long channelId) {
                List<ChannelMessage> messages = messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);

                if (messages.isEmpty()) {
                        return new ArrayList<>();
                }

                // Collect distinct user IDs
                Set<Long> userIds = messages.stream()
                                .map(ChannelMessage::getUserId)
                                .collect(Collectors.toSet());

                // Fetch users from MySQL
                Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                                .collect(Collectors.toMap(User::getId, Function.identity()));

                return messages.stream()
                                .map(msg -> {
                                        User user = userMap.get(msg.getUserId());
                                        return mapToResponse(msg, user);
                                })
                                .collect(Collectors.toList());
        }

        @Override
        public void deleteMessage(String messageId, Long userId) {
                ChannelMessage message = messageRepository.findById(messageId)
                                .orElseThrow(() -> new RuntimeException("Message not found"));

                if (!message.getUserId().equals(userId)) {
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

                if (!message.getUserId().equals(userId)) {
                        throw new RuntimeException("You are not allowed to edit this message");
                }

                message.setContent(newContent);
                message.setEdited(true);
                message.setUpdatedAt(new Date());
                ChannelMessage updated = messageRepository.save(message);

                // Need user info to return response
                User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

                ChatMessageResponse response = mapToResponse(updated, user);

                SocketResponse socketResponse = SocketResponse.builder()
                                .type("EDIT")
                                .data(response)
                                .build();
                messagingTemplate.convertAndSend("/topic/channel/" + message.getChannelId(), socketResponse);

                return response;
        }

        private ChatMessageResponse mapToResponse(ChannelMessage msg, User user) {
                return ChatMessageResponse.builder()
                                .id(msg.getId())
                                .content(msg.getContent())
                                .senderId(msg.getUserId())
                                .senderName(user != null ? user.getDisplayName() : "Unknown User")
                                .senderAvatar(user != null ? user.getAvatarUrl() : null)
                                .createdAt(convertToLocalDateTime(msg.getCreatedAt()))
                                .build();
        }

        private LocalDateTime convertToLocalDateTime(Date dateToConvert) {
                return dateToConvert.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
        }
}
