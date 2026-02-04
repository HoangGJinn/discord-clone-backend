package com.discordclone.backend.service.message;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;
import com.discordclone.backend.entity.jpa.Channel;
import com.discordclone.backend.entity.jpa.Message;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.ChannelRepository;
import com.discordclone.backend.repository.MessageRepository;
import com.discordclone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

        private final MessageRepository messageRepository;
        private final UserRepository userRepository;
        private final ChannelRepository channelRepository;

        @Transactional
        public ChatMessageResponse saveMessage(Long channelId, ChatMessageRequest req) {
                User sender = userRepository.findById(req.getSenderId())
                                .orElseThrow(() -> new RuntimeException("User not found"));
                Channel channel = channelRepository.findById(channelId)
                                .orElseThrow(() -> new RuntimeException("Channel not found"));
                Message message = Message.builder()
                                .content(req.getContent())
                                .user(sender)
                                .channel(channel)
                                .build();

                Message saved = messageRepository.save(message);
                return ChatMessageResponse.builder()
                                .id(saved.getId())
                                .content(saved.getContent())
                                .senderId(sender.getId())
                                .senderName(sender.getDisplayName())
                                .senderAvatar(sender.getAvatarUrl())
                                .createdAt(saved.getCreatedAt())
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public java.util.List<ChatMessageResponse> getMessagesByChannel(Long channelId) {
                return messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId).stream()
                                .map(message -> ChatMessageResponse.builder()
                                                .id(message.getId())
                                                .content(message.getContent())
                                                .senderId(message.getUser().getId())
                                                .senderName(message.getUser().getDisplayName())
                                                .senderAvatar(message.getUser().getAvatarUrl())
                                                .createdAt(message.getCreatedAt())
                                                .build())
                                .collect(java.util.stream.Collectors.toList());
        }
}
