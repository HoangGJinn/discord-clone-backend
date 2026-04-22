package com.discordclone.backend.repository.mongo;

import com.discordclone.backend.entity.mongo.ChannelMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ChannelMessageRepository extends MongoRepository<ChannelMessage, String> {
    List<ChannelMessage> findByChannelIdOrderByCreatedAtAsc(Long channelId);

    java.util.Optional<ChannelMessage> findTopByChannelIdAndSenderIdNotOrderByCreatedAtDesc(Long channelId, Long senderId);

    long countByChannelIdAndSenderIdNot(Long channelId, Long senderId);

    long countByChannelIdAndSenderIdNotAndCreatedAtAfter(Long channelId, Long senderId, Date createdAt);

    // Message search
    Page<ChannelMessage> findByChannelIdAndContentContainingIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(
            Long channelId, String content, Pageable pageable);
}

