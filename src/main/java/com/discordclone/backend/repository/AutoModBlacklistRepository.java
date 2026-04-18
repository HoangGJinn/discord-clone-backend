package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.AutoModBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutoModBlacklistRepository extends JpaRepository<AutoModBlacklist, Long> {
    Optional<AutoModBlacklist> findByKeyword(String keyword);
    boolean existsByKeyword(String keyword);
}
