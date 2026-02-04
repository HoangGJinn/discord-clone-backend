package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // Lấy tin nhắn của channel, sắp xếp cũ nhất trước (để hiển thị từ trên xuống)
    // JOIN FETCH user để lấy dữ liệu user mới nhất (bao gồm displayName đã cập
    // nhật)
    @Query("SELECT m FROM Message m JOIN FETCH m.user WHERE m.channel.id = :channelId ORDER BY m.createdAt ASC")
    List<Message> findByChannelIdOrderByCreatedAtAsc(@Param("channelId") Long channelId);
}
