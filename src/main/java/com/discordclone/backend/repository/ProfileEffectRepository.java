package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.ProfileEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileEffectRepository extends JpaRepository<ProfileEffect, Long> {
    List<ProfileEffect> findByIsActiveTrue();
}
