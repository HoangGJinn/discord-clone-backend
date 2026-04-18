package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.entity.jpa.Warning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarningRepository extends JpaRepository<Warning, Long> {
    List<Warning> findByUserId(Long userId);
    List<Warning> findByUserIdAndIsActiveTrue(Long userId);
    Optional<Warning> findByUserAndIsActiveTrue(User user);
}
