package com.discordclone.backend.service.message;

import com.discordclone.backend.dto.request.ChatMessageRequest;
import com.discordclone.backend.dto.response.ChatMessageResponse;
import com.discordclone.backend.dto.response.SocketResponse;
import com.discordclone.backend.dto.message.MessageAttachment;
import com.discordclone.backend.entity.jpa.Channel;
import com.discordclone.backend.entity.mongo.ChannelMessage;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.entity.jpa.ChannelReadState;
import com.discordclone.backend.repository.ChannelRepository;
import com.discordclone.backend.repository.ChannelReadStateRepository;
import com.discordclone.backend.repository.ServerMemberRepository;
import com.discordclone.backend.repository.UserFcmTokenRepository;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.repository.mongo.ChannelMessageRepository;
import com.discordclone.backend.service.impl.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

        private final ChannelMessageRepository messageRepository;
        private final UserRepository userRepository;
        private final ChannelRepository channelRepository;
        private final ChannelReadStateRepository channelReadStateRepository;
        private final ServerMemberRepository serverMemberRepository;
        private final UserFcmTokenRepository fcmTokenRepository;
        private final FcmService fcmService;
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
                message.setSenderAvatarEffectId(sender.getAvatarEffectId());
                message.setSenderBannerEffectId(sender.getBannerEffectId());
                message.setSenderCardEffectId(sender.getCardEffectId());
                message.setContent(req.getContent());
                message.setReplyToId(req.getReplyToId());
                List<MessageAttachment> attachmentUrls = Optional.ofNullable(req.getAttachments())
                                .orElseGet(ArrayList::new)
                                .stream()
                                .filter(Objects::nonNull)
                                .map(item -> MessageAttachment.builder()
                                        .url(item.getUrl())
                                        .filename(item.getFilename())
                                        .contentType(item.getContentType())
                                        .size(item.getSize())
                                        .build())
                                .filter(item -> item.getUrl() != null && !item.getUrl().isBlank())
                                .collect(Collectors.toList());
                message.setAttachments(attachmentUrls);
                message.setEdited(false);
                message.setDeleted(false);
                message.setPinned(false);
                message.setCreatedAt(now);
                message.setUpdatedAt(now);

                ChannelMessage saved = messageRepository.save(message);

                // Gửi FCM notification bất đồng bộ đến các thành viên trong server
                sendServerChannelNotification(saved);

                return mapToResponse(saved, true);
        }

        @Override
        public List<ChatMessageResponse> getMessagesByChannel(Long channelId) {
                List<ChannelMessage> messages = messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);
                return messages.stream()
                                .map(msg -> mapToResponse(msg, true))
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

                ChatMessageResponse response = mapToResponse(updated, true);

                SocketResponse socketResponse = SocketResponse.builder()
                                .type("EDIT")
                                .data(response)
                                .build();
                messagingTemplate.convertAndSend("/topic/channel/" + message.getChannelId(), socketResponse);

                return response;
        }

        @Override
        public void addReaction(String messageId, Long userId, String emoji) {
                ChannelMessage message = messageRepository.findById(messageId)
                                .orElseThrow(() -> new RuntimeException("Message not found"));

                boolean alreadyReacted = message.getReactions().stream()
                                .anyMatch(r -> r.getUserId().equals(userId) && r.getEmoji().equals(emoji));

                if (!alreadyReacted) {
                        ChannelMessage.Reaction reaction = new ChannelMessage.Reaction();
                        reaction.setUserId(userId);
                        reaction.setEmoji(emoji);
                        List<ChannelMessage.Reaction> updated = new ArrayList<>(message.getReactions());
                        updated.add(reaction);
                        message.setReactions(updated);
                        message.setUpdatedAt(new Date());
                        ChannelMessage saved = messageRepository.save(message);
                        messagingTemplate.convertAndSend(
                                "/topic/channel/" + saved.getChannelId(),
                                mapToResponse(saved, true)
                        );
                }
        }

        @Override
        public void removeReaction(String messageId, Long userId, String emoji) {
                ChannelMessage message = messageRepository.findById(messageId)
                                .orElseThrow(() -> new RuntimeException("Message not found"));

                List<ChannelMessage.Reaction> filtered = message.getReactions().stream()
                                .filter(r -> !(r.getUserId().equals(userId) && r.getEmoji().equals(emoji)))
                                .collect(Collectors.toList());

                message.setReactions(filtered);
                message.setUpdatedAt(new Date());
                ChannelMessage saved = messageRepository.save(message);
                messagingTemplate.convertAndSend(
                        "/topic/channel/" + saved.getChannelId(),
                        mapToResponse(saved, true)
                );
        }

        @Override
        public void markChannelAsRead(Long channelId, Long userId) {
                Channel channel = channelRepository.findById(channelId)
                        .orElseThrow(() -> new RuntimeException("Channel not found"));
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                ChannelReadState readState = channelReadStateRepository
                        .findByChannelIdAndUserId(channelId, userId)
                        .orElseGet(() -> ChannelReadState.builder()
                                .channel(channel)
                                .user(user)
                                .build());

                readState.setLastReadAt(java.time.LocalDateTime.now());
                channelReadStateRepository.save(readState);
        }

        @Override
        public void markChannelAsUnread(Long channelId, Long userId) {
                com.discordclone.backend.entity.jpa.Channel channel = channelRepository.findById(channelId)
                        .orElseThrow(() -> new RuntimeException("Channel not found"));
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                ChannelReadState readState = channelReadStateRepository
                        .findByChannelIdAndUserId(channelId, userId)
                        .orElseGet(() -> ChannelReadState.builder()
                                .channel(channel)
                                .user(user)
                                .build());

                Date targetReadAt = messageRepository
                        .findTopByChannelIdAndSenderIdNotOrderByCreatedAtDesc(channelId, userId)
                        .map(message -> new Date(message.getCreatedAt().getTime() - 1))
                        .orElseGet(() -> new Date(System.currentTimeMillis() - 1000));
                readState.setLastReadAt(targetReadAt.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                channelReadStateRepository.save(readState);
        }

        private ChatMessageResponse mapToResponse(ChannelMessage msg, boolean includeReply) {
                ChatMessageResponse replyToMessage = null;
                if (includeReply && msg.getReplyToId() != null && !msg.getReplyToId().isBlank()) {
                        replyToMessage = messageRepository.findById(msg.getReplyToId())
                                .map(reply -> mapToResponse(reply, false))
                                .orElse(null);
                }

                return ChatMessageResponse.builder()
                                .id(msg.getId())
                                .channelId(msg.getChannelId())
                                .senderId(msg.getSenderId())
                                .senderName(msg.getSenderName())
                                .senderAvatar(msg.getSenderAvatar())
                                .senderAvatarEffectId(msg.getSenderAvatarEffectId())
                                .senderBannerEffectId(msg.getSenderBannerEffectId())
                                .senderCardEffectId(msg.getSenderCardEffectId())
                                .content(msg.getContent())
                                .attachments(msg.getAttachments() != null ? msg.getAttachments() : Collections.emptyList())
                                .replyToId(msg.getReplyToId())
                                .replyToMessage(replyToMessage)
                                .edited(msg.getEdited())
                                .deleted(msg.getDeleted())
                                .pinned(msg.getPinned())
                                .createdAt(msg.getCreatedAt())
                                .updatedAt(msg.getUpdatedAt())
                                .reactions(msg.getReactions())
                                .build();
        }

        /**
         * Lấy tất cả thành viên trong server chứa channel này,
         * loại người gửi ra, lấy FCM tokens, rồi bắn notification.
         */
        private void sendServerChannelNotification(ChannelMessage saved) {
                try {
                        Channel channel = channelRepository.findById(saved.getChannelId())
                                .orElse(null);
                        if (channel == null || channel.getServer() == null) return;

                        Long serverId = channel.getServer().getId();

                        // Lấy userId tất cả member trong server, trừ người gửi
                        List<Long> recipientIds = serverMemberRepository
                                .findByServerId(serverId)
                                .stream()
                                .map(m -> m.getUser().getId())
                                .filter(uid -> !uid.equals(saved.getSenderId()))
                                .collect(Collectors.toList());

                        if (recipientIds.isEmpty()) return;

                        List<String> tokens = fcmTokenRepository.findFcmTokensByUserIds(recipientIds);
                        if (tokens.isEmpty()) return;

                        fcmService.sendServerMessageNotification(
                                tokens,
                                saved.getSenderName(),
                                channel.getName(),
                                String.valueOf(serverId),
                                String.valueOf(saved.getChannelId()),
                                saved.getContent()
                        );
                } catch (Exception e) {
                        // FCM lỗi không được ảnh hưởng flow chính
                        System.err.println("[FCM] sendServerChannelNotification failed: " + e.getMessage());
                }
        }
}
