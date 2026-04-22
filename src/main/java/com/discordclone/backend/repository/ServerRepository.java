package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.Server;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long>, JpaSpecificationExecutor<Server> {

    // Tìm tất cả server mà user là owner
    List<Server> findByOwnerId(Long ownerId);

    // Tìm server bằng invite code
    Optional<Server> findByInviteCode(String inviteCode);

    // Kiểm tra invite code đã tồn tại chưa
    boolean existsByInviteCode(String inviteCode);

    // Search gần đúng theo name hoặc description (fuzzy search)
    @Query("SELECT s FROM Server s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Server> searchByKeyword(@Param("keyword") String keyword);

    // Search chỉ theo name
    List<Server> findByNameContainingIgnoreCase(String name);

    // Eager fetch members để admin xem chi tiết
    @EntityGraph(attributePaths = {"owner", "members", "members.user"})
    @Query("SELECT s FROM Server s WHERE s.id = :id")
    Optional<Server> findByIdWithMembers(@Param("id") Long id);

    @Query("SELECT s FROM Server s LEFT JOIN s.members sm GROUP BY s.id ORDER BY COUNT(sm) DESC")
    List<Server> findTopServersByMemberCount(org.springframework.data.domain.Pageable pageable);

    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
