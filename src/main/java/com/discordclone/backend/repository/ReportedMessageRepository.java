package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.ReportedMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportedMessageRepository extends JpaRepository<ReportedMessage, Long>, JpaSpecificationExecutor<ReportedMessage> {
    Page<ReportedMessage> findByStatus(ReportedMessage.ReportStatus status, Pageable pageable);
    Optional<ReportedMessage> findByMessageId(String messageId);

    Long countByStatus(ReportedMessage.ReportStatus status);
}
