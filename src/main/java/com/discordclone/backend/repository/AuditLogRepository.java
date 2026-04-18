package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findByAdminId(Long adminId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);
}
