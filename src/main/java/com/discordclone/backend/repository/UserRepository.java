package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUserName(String userName);

    Optional<User> findByEmail(String email);

    boolean existsByUserName(String userName);

    boolean existsByEmail(String email);

    // Tìm kiếm user theo username hoặc displayName (case-insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    // Admin queries
    Long countByIsActiveTrue();

    Long countByIsActiveFalse();

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastActive > :since")
    Long countByLastActiveAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startOfDay")
    Long countByCreatedAtAfter(@Param("startOfDay") LocalDateTime startOfDay);

    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Count friends for a user
    @Query("SELECT COUNT(f) FROM Friendship f WHERE (f.sender.id = :userId OR f.receiver.id = :userId) AND f.status = 'ACCEPTED'")
    Integer countFriendsByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count FROM users WHERE created_at >= :startDate GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> countUsersGroupedByDateNative(@Param("startDate") LocalDateTime startDate);
}
