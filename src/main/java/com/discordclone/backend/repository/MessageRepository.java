package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // Lấy tin nhắn của channel, sắp xếp cũ nhất trước (để hiển thị từ trên xuống)
    List<Message> findByChannelIdOrderByCreatedAtAsc(Long channelId);
}
