package com.discordclone.backend.repository.mongo;

import com.discordclone.backend.entity.mongo.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Date;

@Repository
public interface DirectMessageRepository extends MongoRepository<DirectMessage, String> {

    Page<DirectMessage> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDesc(String conversationId);

    Optional<DirectMessage> findTopByConversationIdAndSenderIdNotOrderByCreatedAtDesc(String conversationId, Long senderId);

    long countByConversationIdAndSenderIdNot(String conversationId, Long senderId);

    long countByConversationIdAndSenderIdNotAndCreatedAtAfter(String conversationId, Long senderId, Date createdAt);
}
