package com.discordclone.backend.service.directmessage;

import com.discordclone.backend.dto.request.DirectMessageRequest;
import com.discordclone.backend.dto.request.EditMessageRequest;
import com.discordclone.backend.dto.message.MessageAttachment;
import com.discordclone.backend.dto.response.ConversationResponse;
import com.discordclone.backend.dto.response.DirectMessageResponse;
import com.discordclone.backend.dto.response.UserResponse;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.entity.mongo.Conversation;
import com.discordclone.backend.entity.mongo.DirectMessage;
import com.discordclone.backend.repository.UserRepository;
import com.discordclone.backend.repository.mongo.ConversationRepository;
import com.discordclone.backend.repository.mongo.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Override
    public DirectMessageResponse sendMessage(Long senderId, DirectMessageRequest request) {
        // Get or create conversation
        ConversationResponse conv = getOrCreateConversation(senderId, request.getReceiverId());

        List<MessageAttachment> attachments = Optional.ofNullable(request.getAttachments())
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
            .toList();

        // Build and save message
        DirectMessage message = DirectMessage.builder()
                .conversationId(conv.getId())
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .content(request.getContent())
                .replyToId(request.getReplyToId())
                .createdAt(new Date())
                .updatedAt(new Date())
                .isRead(false)
                .edited(false)
                .deleted(false)
                .reactions(new HashMap<>())
                .build();

        message.setAttachments(attachments);

        DirectMessage saved = directMessageRepository.save(message);

        DirectMessageResponse replyTo = null;
        if (saved.getReplyToId() != null) {
            replyTo = directMessageRepository.findById(saved.getReplyToId())
                .map(r -> mapToResponse(r, null))
                .orElse(null);
        }

        // Update conversation updatedAt
        conversationRepository.findById(conv.getId()).ifPresent(c -> {
            c.setUpdatedAt(new Date());
            conversationRepository.save(c);
        });

        return mapToResponse(saved, replyTo);
    }

    @Override
    public Page<DirectMessageResponse> getMessages(String conversationId, Long userId, Pageable pageable) {
        // Verify user is part of conversation
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new RuntimeException("User not authorized to view this conversation");
        }

        return directMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(dm -> {
                    DirectMessageResponse replyTo = null;
                    if (dm.getReplyToId() != null) {
                        replyTo = directMessageRepository.findById(dm.getReplyToId())
                                .map(r -> mapToResponse(r, null))
                                .orElse(null);
                    }
                    return mapToResponse(dm, replyTo);
                });
    }

    @Override
    public ConversationResponse getOrCreateConversation(Long userId1, Long userId2) {
        Long normalizedUser1 = Math.min(userId1, userId2);
        Long normalizedUser2 = Math.max(userId1, userId2);

        Optional<Conversation> existing = conversationRepository.findByUser1IdAndUser2Id(normalizedUser1, normalizedUser2);

        Conversation conv;
        if (existing.isPresent()) {
            conv = existing.get();
        } else {
            try {
                conv = Conversation.builder()
                        .user1Id(normalizedUser1)
                        .user2Id(normalizedUser2)
                        .createdAt(new Date())
                        .updatedAt(new Date())
                        .user1LastReadAt(new Date())
                        .user2LastReadAt(new Date())
                        .build();
                conv = conversationRepository.save(conv);
            } catch (DuplicateKeyException ex) {
                conv = conversationRepository.findByUser1IdAndUser2Id(normalizedUser1, normalizedUser2)
                        .orElseThrow(() -> ex);
            }
        }

        User user1 = userRepository.findById(conv.getUser1Id()).orElse(null);
        User user2 = userRepository.findById(conv.getUser2Id()).orElse(null);
        
        Long otherUserId = conv.getUser1Id().equals(userId1) ? conv.getUser2Id() : conv.getUser1Id();
        User otherUser = userRepository.findById(otherUserId).orElse(null);

        return ConversationResponse.builder()
                .id(conv.getId())
                .participantOne(UserResponse.from(user1))
                .participantTwo(UserResponse.from(user2))
                .user1Id(conv.getUser1Id())
                .user2Id(conv.getUser2Id())
                .otherUserId(otherUserId)
                .otherUserName(otherUser != null ? otherUser.getDisplayName() : null)
                .otherUserAvatar(otherUser != null ? otherUser.getAvatarUrl() : null)
                .unreadCount(0L)
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    @Override
    public List<ConversationResponse> getConversationList(Long userId) {
        List<Conversation> conversations = conversationRepository.findAllByUserId(userId);
        List<ConversationResponse> result = new ArrayList<>();

        for (Conversation conv : conversations) {
            ConversationResponse resp = getOrCreateConversation(conv.getUser1Id(), conv.getUser2Id());
            
            // Get last message
            directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conv.getId())
                    .ifPresent(lastMsg -> resp.setLastMessage(mapToResponse(lastMsg, null)));

            Date lastReadAt = Objects.equals(conv.getUser1Id(), userId)
                    ? conv.getUser1LastReadAt()
                    : conv.getUser2LastReadAt();
            long unreadCount = lastReadAt == null
                    ? directMessageRepository.countByConversationIdAndSenderIdNot(conv.getId(), userId)
                    : directMessageRepository.countByConversationIdAndSenderIdNotAndCreatedAtAfter(
                            conv.getId(),
                            userId,
                            lastReadAt
                    );
            resp.setUnreadCount(unreadCount);

            result.add(resp);
        }

        // Sort by weight: lastMessage.createdAt descending if exists, else updatedAt
        result.sort((a, b) -> {
            Date dateA = a.getLastMessage() != null ? a.getLastMessage().getCreatedAt() : a.getUpdatedAt();
            Date dateB = b.getLastMessage() != null ? b.getLastMessage().getCreatedAt() : b.getUpdatedAt();
            return dateB.compareTo(dateA);
        });

        return result;
    }

    @Override
    public void markConversationAsRead(String conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!Objects.equals(conversation.getUser1Id(), userId) && !Objects.equals(conversation.getUser2Id(), userId)) {
            throw new RuntimeException("User not authorized to mark this conversation as read");
        }

        Date now = new Date();
        if (Objects.equals(conversation.getUser1Id(), userId)) {
            conversation.setUser1LastReadAt(now);
        } else {
            conversation.setUser2LastReadAt(now);
        }
        conversationRepository.save(conversation);
    }

    @Override
    public void markConversationAsUnread(String conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!Objects.equals(conversation.getUser1Id(), userId) && !Objects.equals(conversation.getUser2Id(), userId)) {
            throw new RuntimeException("User not authorized to mark this conversation as unread");
        }

        Date targetReadAt = directMessageRepository
                .findTopByConversationIdAndSenderIdNotOrderByCreatedAtDesc(conversationId, userId)
                .map(message -> new Date(message.getCreatedAt().getTime() - 1))
                .orElseGet(() -> new Date(System.currentTimeMillis() - 1000));

        if (Objects.equals(conversation.getUser1Id(), userId)) {
            conversation.setUser1LastReadAt(targetReadAt);
        } else {
            conversation.setUser2LastReadAt(targetReadAt);
        }
        conversationRepository.save(conversation);
    }

    @Override
    public DirectMessageResponse editMessage(String messageId, Long userId, EditMessageRequest request) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this message");
        }

        message.setContent(request.getContent());
        message.setEdited(true);
        message.setUpdatedAt(new Date());

        DirectMessage saved = directMessageRepository.save(message);
        return mapToResponse(saved, null);
    }

    @Override
    public DirectMessageResponse deleteMessage(String messageId, Long userId) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this message");
        }

        message.setDeleted(true);
        message.setContent("[Message deleted]");
        message.setUpdatedAt(new Date());

        DirectMessage saved = directMessageRepository.save(message);
        return mapToResponse(saved, null);
    }

    @Override
    public DirectMessageResponse addReaction(String messageId, Long userId, String emoji) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        Map<String, Set<Long>> reactions = message.getReactions();
        if (reactions == null) {
            reactions = new HashMap<>();
        }

        reactions.computeIfAbsent(emoji, k -> new HashSet<>()).add(userId);
        message.setReactions(reactions);
        DirectMessage saved = directMessageRepository.save(message);
        return mapToResponse(saved, null);
    }

    @Override
    public DirectMessageResponse removeReaction(String messageId, Long userId, String emoji) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        Map<String, Set<Long>> reactions = message.getReactions();
        if (reactions != null) {
            Set<Long> users = reactions.get(emoji);
            if (users != null) {
                users.remove(userId);
                if (users.isEmpty()) {
                    reactions.remove(emoji);
                } else {
                    reactions.put(emoji, users);
                }
            }
            message.setReactions(reactions);
            DirectMessage saved = directMessageRepository.save(message);
            return mapToResponse(saved, null);
        }

        return mapToResponse(message, null);
    }

    private DirectMessageResponse mapToResponse(DirectMessage dm, DirectMessageResponse replyTo) {
        User sender = null;
        User receiver = null;

        if (dm.getSenderId() != null) {
            sender = userRepository.findById(dm.getSenderId()).orElse(null);
        }
        if (dm.getReceiverId() != null) {
            receiver = userRepository.findById(dm.getReceiverId()).orElse(null);
        }

        return DirectMessageResponse.builder()
                .id(dm.getId())
                .conversationId(dm.getConversationId())
                .senderId(dm.getSenderId())
                .receiverId(dm.getReceiverId())
                .sender(UserResponse.from(sender))
                .receiver(UserResponse.from(receiver))
                .content(dm.getContent())
                .createdAt(dm.getCreatedAt())
                .updatedAt(dm.getUpdatedAt())
                .edited(dm.isEdited())
                .deleted(dm.isDeleted())
                .isRead(dm.isRead())
                .attachments(dm.getAttachments() != null ? dm.getAttachments() : Collections.emptyList())
                .replyToId(dm.getReplyToId())
                .replyToMessage(replyTo)
                .reactions(dm.getReactions())
                .build();
    }
}
