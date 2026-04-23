package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    // Lấy tất cả token của một user (gửi notification đến mọi thiết bị)
    @Query("SELECT t.fcmToken FROM UserFcmToken t WHERE t.user.id = :userId")
    List<String> findFcmTokensByUserId(@Param("userId") Long userId);

    // Lấy tất cả token của nhiều user cùng lúc (gửi server channel notification)
    @Query("SELECT t.fcmToken FROM UserFcmToken t WHERE t.user.id IN :userIds")
    List<String> findFcmTokensByUserIds(@Param("userIds") List<Long> userIds);

    // Tìm token theo userId + deviceId để upsert
    Optional<UserFcmToken> findByUserIdAndDeviceId(Long userId, String deviceId);

    // Xóa token theo deviceId khi logout
    @Modifying
    @Query("DELETE FROM UserFcmToken t WHERE t.user.id = :userId AND t.deviceId = :deviceId")
    void deleteByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    // Xóa tất cả token của user (xóa tài khoản / force logout all)
    @Modifying
    @Query("DELETE FROM UserFcmToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    // Xóa một fcmToken cụ thể (dùng khi FCM trả về token không hợp lệ)
    @Modifying
    @Query("DELETE FROM UserFcmToken t WHERE t.fcmToken = :fcmToken")
    void deleteByFcmToken(@Param("fcmToken") String fcmToken);
}
