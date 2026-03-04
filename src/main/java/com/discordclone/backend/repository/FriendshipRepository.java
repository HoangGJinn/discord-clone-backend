package com.discordclone.backend.repository;

import com.discordclone.backend.entity.enums.FriendshipStatus;
import com.discordclone.backend.entity.jpa.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // Tìm friendship giữa 2 user (theo cả 2 chiều)
    @Query("SELECT f FROM Friendship f WHERE (f.sender.id = :userId1 AND f.receiver.id = :userId2) OR (f.sender.id = :userId2 AND f.receiver.id = :userId1)")
    Optional<Friendship> findBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // Danh sách lời mời đang chờ mà user NHẬN được
    List<Friendship> findByReceiverIdAndStatus(Long receiverId, FriendshipStatus status);

    // Danh sách lời mời user ĐÃ GỬI đang chờ
    List<Friendship> findBySenderIdAndStatus(Long senderId, FriendshipStatus status);

    // Lấy tất cả bạn bè của user (đã ACCEPTED, là sender HOẶC receiver)
    @Query("SELECT f FROM Friendship f WHERE (f.sender.id = :userId OR f.receiver.id = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAllFriendsOf(@Param("userId") Long userId);

    // Kiểm tra 2 user đã là bạn chưa
    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE ((f.sender.id = :userId1 AND f.receiver.id = :userId2) OR (f.sender.id = :userId2 AND f.receiver.id = :userId1)) AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // Đếm số bạn bè
    @Query("SELECT COUNT(f) FROM Friendship f WHERE (f.sender.id = :userId OR f.receiver.id = :userId) AND f.status = 'ACCEPTED'")
    long countFriendsOf(@Param("userId") Long userId);
}
