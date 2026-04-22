package com.discordclone.backend.service.admin;

import com.discordclone.backend.dto.response.*;
import com.discordclone.backend.entity.jpa.AuditLog;
import com.discordclone.backend.entity.jpa.NitroOrder;
import com.discordclone.backend.entity.jpa.ReportedMessage;
import com.discordclone.backend.entity.jpa.Server;
import com.discordclone.backend.entity.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminService {
    // Stats
    AdminStatsOverview getOverviewStats();

    List<UserGrowthData> getUserGrowth(LocalDate from, LocalDate to);

    List<DailyActiveUsers> getDailyActiveUsers(int days);

    List<AdminServerSummary> getTopServers(int limit);

    // User Management
    Page<AdminUserSummary> getAllUsers(Specification<User> spec, Pageable pageable);

    void disableUser(Long userId, Long adminId, String ipAddress);

    void enableUser(Long userId, Long adminId, String ipAddress);

    void banUser(Long userId, String reason, Long adminId, String ipAddress);

    void unbanUser(Long userId, Long adminId, String ipAddress);

    void bulkDisableUsers(List<Long> userIds, String reason, Long adminId, String ipAddress);

    void bulkBanUsers(List<Long> userIds, String reason, Long adminId, String ipAddress);

    // Server Management
    Page<AdminServerSummary> getAllServers(Specification<Server> spec, Pageable pageable);

    void deleteServer(Long serverId, Long adminId, String ipAddress);

    void banServer(Long serverId, String reason, Long adminId, String ipAddress);

    void unbanServer(Long serverId, Long adminId, String ipAddress);

    // Report Management
    Page<ReportedMessageResponse> getAllReports(Specification<ReportedMessage> spec, Pageable pageable);

    ReportedMessageResponse getReportById(Long reportId);

    void resolveReport(Long reportId, String action, Long adminId, String ipAddress);

    void deleteReport(Long reportId);

    // Moderation Actions
    void deleteMessage(String messageId);

    void warnUser(Long userId, String reason, Long adminId, String ipAddress);

    void banUserPermanently(Long userId, String reason, Long adminId, String ipAddress);

    // Blacklist Management
    List<BlacklistKeywordResponse> getBlacklist();

    BlacklistKeywordResponse addBlacklistKeyword(String keyword, Long adminId, String ipAddress);

    void removeBlacklistKeyword(Long blacklistId, Long adminId, String ipAddress);

    boolean isMessageContainsBlacklistedWord(String content);

    // Audit Logs
    Page<AuditLogResponse> getAuditLogs(Specification<AuditLog> spec, Pageable pageable);

    AuditLogResponse getAuditLogById(Long logId);

    void logAudit(AuditLog auditLog);

    // Nitro Payment Admin
    Page<NitroOrderSummary> getAllOrders(Specification<NitroOrder> spec, Pageable pageable);

    NitroOrderSummary getOrderByTxnRef(String txnRef);

    void approveOrder(String txnRef, Long adminId, String ipAddress);

    void rejectOrder(String txnRef, Long adminId, String ipAddress);

    Map<String, Object> getRevenueStats();
}
