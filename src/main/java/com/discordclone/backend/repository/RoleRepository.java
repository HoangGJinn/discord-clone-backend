package com.discordclone.backend.repository;

import com.discordclone.backend.entity.enums.ERole;
import com.discordclone.backend.entity.jpa.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}
