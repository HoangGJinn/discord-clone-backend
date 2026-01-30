package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    // Tìm tất cả server mà user là owner
    List<Server> findByOwnerId(Long ownerId);

    // Tìm server bằng invite code
    Optional<Server> findByInviteCode(String inviteCode);

    // Kiểm tra invite code đã tồn tại chưa
    boolean existsByInviteCode(String inviteCode);
}
