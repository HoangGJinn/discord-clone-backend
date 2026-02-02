package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Lấy tất cả categories của một server, sắp xếp theo position
    List<Category> findByServerIdOrderByPositionAsc(Long serverId);

    // Đếm số category trong server (để tính position mới)
    long countByServerId(Long serverId);

    // Search gần đúng theo name trong server
    List<Category> findByServerIdAndNameContainingIgnoreCase(Long serverId, String name);

    // Search gần đúng theo name (toàn bộ)
    List<Category> findByNameContainingIgnoreCase(String name);
}
