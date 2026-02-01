package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {

    // Lấy tất cả members của một server
    List<ServerMember> findByServerId(Long serverId);

    // Lấy tất cả servers mà user là thành viên
    List<ServerMember> findByUserId(Long userId);

    // Tìm membership của user trong server cụ thể
    Optional<ServerMember> findByServerIdAndUserId(Long serverId, Long userId);

    // Kiểm tra user đã là thành viên server chưa
    boolean existsByServerIdAndUserId(Long serverId, Long userId);

    // Xóa membership của user khỏi server
    void deleteByServerIdAndUserId(Long serverId, Long userId);

    // Đếm số thành viên trong server
    long countByServerId(Long serverId);

    // Search gần đúng member trong server theo displayName, userName hoặc nickname
    @Query("SELECT m FROM ServerMember m WHERE m.server.id = :serverId AND (" +
            "LOWER(m.user.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.user.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ServerMember> searchMembersInServer(@Param("serverId") Long serverId, @Param("keyword") String keyword);
}
