package com.discordclone.backend.repository.mongo;

import com.discordclone.backend.entity.mongo.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    @Query("{ $or: [ { 'user1Id': ?0, 'user2Id': ?1 }, { 'user1Id': ?1, 'user2Id': ?0 } ] }")
    Optional<Conversation> findByUsers(Long userId1, Long userId2);

    @Query("{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }")
    List<Conversation> findAllByUserId(Long userId);
}
