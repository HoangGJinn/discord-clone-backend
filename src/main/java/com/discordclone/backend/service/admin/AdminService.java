package com.discordclone.backend.service.admin;

import com.discordclone.backend.dto.response.*;
import com.discordclone.backend.entity.jpa.AuditLog;
import com.discordclone.backend.entity.jpa.ReportedMessage;
import com.discordclone.backend.entity.jpa.Server;
import com.discordclone.backend.entity.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {
    // Stats
    AdminStatsOverview getOverviewStats();

    List<UserGrowthData> getUserGrowth(LocalDate from, LocalDate to);

    List<DailyActiveUsers> getDailyActiveUsers(int days);

    List<AdminServerSummary> getTopServers(int limit);

    // User Management
    Page<AdminUserSummary> getAllUsers(Specification<User> spec, Pageable pageable);

    void disableUser(Long userId);

    void enableUser(Long userId);

    void banUser(Long userId, String reason);

    void unbanUser(Long userId);

    void bulkDisableUsers(List<Long> userIds, String reason);

    void bulkBanUsers(List<Long> userIds, String reason);

    // Server Management
    Page<AdminServerSummary> getAllServers(Specification<Server> spec, Pageable pageable);

    void deleteServer(Long serverId);

    // Report Management
    Page<ReportedMessageResponse> getAllReports(Specification<ReportedMessage> spec, Pageable pageable);

    ReportedMessageResponse getReportById(Long reportId);

    void resolveReport(Long reportId, String action, Long adminId, String ipAddress);

    void deleteReport(Long reportId);

    // Moderation Actions
    void deleteMessage(String messageId);

    void warnUser(Long userId, String reason, Long adminId);

    void banUserPermanently(Long userId, String reason, Long adminId);

    // Blacklist Management
    List<BlacklistKeywordResponse> getBlacklist();

    BlacklistKeywordResponse addBlacklistKeyword(String keyword, Long adminId);

    void removeBlacklistKeyword(Long blacklistId);

    boolean isMessageContainsBlacklistedWord(String content);

    // Audit Logs
    Page<AuditLogResponse> getAuditLogs(Specification<AuditLog> spec, Pageable pageable);

    void logAudit(AuditLog auditLog);
}
