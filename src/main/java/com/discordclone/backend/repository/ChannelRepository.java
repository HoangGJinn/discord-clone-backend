package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {

    // Lấy tất cả channels của một server, sắp xếp theo position
    List<Channel> findByServerIdOrderByPositionAsc(Long serverId);

    // Lấy channels thuộc một category
    List<Channel> findByCategoryIdOrderByPositionAsc(Long categoryId);

    // Lấy channels không thuộc category nào (nằm trực tiếp trong server)
    List<Channel> findByServerIdAndCategoryIsNullOrderByPositionAsc(Long serverId);

    // Đếm số channel trong server
    long countByServerId(Long serverId);

    // Đếm số channel trong category
    long countByCategoryId(Long categoryId);

    // Search gần đúng theo name trong server
    List<Channel> findByServerIdAndNameContainingIgnoreCase(Long serverId, String name);

    // Search gần đúng theo name (toàn bộ)
    List<Channel> findByNameContainingIgnoreCase(String name);
}
