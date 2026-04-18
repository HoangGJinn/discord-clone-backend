package com.discordclone.backend.service.admin;

import com.discordclone.backend.dto.response.*;
import com.discordclone.backend.entity.jpa.*;
import com.discordclone.backend.entity.mongo.ChannelMessage;
import com.discordclone.backend.exception.ResourceNotFoundException;
import com.discordclone.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ReportedMessageRepository reportedMessageRepository;
    private final AutoModBlacklistRepository autoModBlacklistRepository;
    private final AuditLogRepository auditLogRepository;
    private final WarningRepository warningRepository;
    private final NitroOrderRepository nitroOrderRepository;

    // ===== STATS =====

    @Override
    public AdminStatsOverview getOverviewStats() {
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countByIsActiveTrue();
        Long totalServers = serverRepository.count();
        Long totalMessages = 0L; // TODO: Count from MongoDB
        Long totalRevenue = nitroOrderRepository.sumConfirmedRevenue();
        Long dailyActiveUsersToday = userRepository.countByLastActiveAfter(LocalDateTime.now().minusDays(1));
        Long newUsersToday = userRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());
        Long totalReports = reportedMessageRepository.count();
        Long pendingReports = reportedMessageRepository.countByStatus(ReportedMessage.ReportStatus.PENDING);

        return AdminStatsOverview.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalServers(totalServers)
                .totalMessages(totalMessages)
                .totalRevenue(totalRevenue)
                .dailyActiveUsersToday(dailyActiveUsersToday)
                .newUsersToday(newUsersToday)
                .totalReports(totalReports)
                .pendingReports(pendingReports)
                .build();
    }

    @Override
    public List<UserGrowthData> getUserGrowth(LocalDate from, LocalDate to) {
        List<Object[]> results = userRepository.countUsersGroupedByDateNative(from.atStartOfDay());
        return results.stream().map(obj -> {
            java.sql.Date date = (java.sql.Date) obj[0];
            Number count = (Number) obj[1];
            return UserGrowthData.builder()
                    .date(date.toLocalDate())
                    .newUsers(count.longValue())
                    .activeUsers(count.longValue()) // Mock active users as new users for now
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<DailyActiveUsers> getDailyActiveUsers(int days) {
        // TODO: Implement
        return List.of();
    }

    @Override
    public List<AdminServerSummary> getTopServers(int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        List<Server> servers = serverRepository.findTopServersByMemberCount(pageable);
        return servers.stream()
                .map(server -> AdminServerSummary.builder()
                        .serverId(server.getId())
                        .name(server.getName())
                        .description(server.getDescription())
                        .iconUrl(server.getIconUrl())
                        .ownerId(server.getOwner().getId())
                        .ownerName(server.getOwner().getUserName())
                        .memberCount(server.getMembers().size())
                        .channelCount(server.getChannels() != null ? server.getChannels().size() : 0)
                        .createdAt(server.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    // ===== USER MANAGEMENT =====

    @Override
    public Page<AdminUserSummary> getAllUsers(Specification<User> spec, Pageable pageable) {
        List<User> users = userRepository.findAll(spec);
        List<AdminUserSummary> summaries = users.stream()
                .map(user -> AdminUserSummary.builder()
                        .userId(user.getId())
                        .userName(user.getUserName())
                        .displayName(user.getDisplayName())
                        .email(user.getEmail())
                        .isActive(user.getIsActive())
                        .isEmailVerified(user.getIsEmailVerified())
                        .createdAt(user.getCreatedAt())
                        .serverCount(0) // TODO: Calculate
                        .friendCount(userRepository.countFriendsByUserId(user.getId()))
                        .build())
                .toList();

        // Simple pagination (should use proper query with count)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), summaries.size());
        List<AdminUserSummary> pageContent = start <= end ? summaries.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, summaries.size());
    }

    @Override
    @Transactional
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void banUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);

        // TODO: Create warning record
    }

    @Override
    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void bulkDisableUsers(List<Long> userIds, String reason) {
        userRepository.findAllById(userIds).forEach(user -> user.setIsActive(false));
        userRepository.saveAll(userRepository.findAllById(userIds));
    }

    @Override
    @Transactional
    public void bulkBanUsers(List<Long> userIds, String reason) {
        userRepository.findAllById(userIds).forEach(user -> user.setIsActive(false));
        userRepository.saveAll(userRepository.findAllById(userIds));
    }

    // ===== SERVER MANAGEMENT =====

    @Override
    public Page<AdminServerSummary> getAllServers(Specification<Server> spec, Pageable pageable) {
        List<Server> servers = serverRepository.findAll(spec);
        List<AdminServerSummary> summaries = servers.stream()
                .map(server -> AdminServerSummary.builder()
                        .serverId(server.getId())
                        .name(server.getName())
                        .description(server.getDescription())
                        .iconUrl(server.getIconUrl())
                        .ownerId(server.getOwner().getId())
                        .ownerName(server.getOwner().getUserName())
                        .memberCount(server.getMembers().size())
                        .channelCount(server.getChannels() != null ? server.getChannels().size() : 0)
                        .createdAt(server.getCreatedAt())
                        .build())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), summaries.size());
        List<AdminServerSummary> pageContent = start <= end ? summaries.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, summaries.size());
    }

    @Override
    @Transactional
    public void deleteServer(Long serverId) {
        Server server = serverRepository.findByIdWithMembers(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server not found"));
        serverRepository.delete(server);
    }

    // ===== REPORTED MESSAGES =====

    @Override
    public Page<ReportedMessageResponse> getAllReports(Specification<ReportedMessage> spec, Pageable pageable) {
        // Simple implementation - return all reports
        List<ReportedMessage> reports = reportedMessageRepository.findAll(spec);
        List<ReportedMessageResponse> responses = reports.stream()
                .map(report -> ReportedMessageResponse.builder()
                        .reportId(report.getId())
                        .messageId(report.getMessageId())
                        .reportedByUserId(report.getReportedBy().getId())
                        .reportedByUserName(report.getReportedBy().getUserName())
                        .messageContent("") // TODO: Fetch from MongoDB
                        .channelName("")
                        .serverName("")
                        .reason(report.getReason())
                        .description(report.getDescription())
                        .status(report.getStatus())
                        .reviewedByAdminId(report.getReviewedByAdmin() != null ? report.getReviewedByAdmin().getId() : null)
                        .reviewedByAdminName(report.getReviewedByAdmin() != null ? report.getReviewedByAdmin().getUserName() : null)
                        .reviewedAt(report.getReviewedAt())
                        .createdAt(report.getCreatedAt())
                        .build())
                .toList();

        return new PageImpl<>(responses, pageable, responses.size());
    }

    @Override
    public ReportedMessageResponse getReportById(Long reportId) {
        ReportedMessage report = reportedMessageRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        return ReportedMessageResponse.builder()
                .reportId(report.getId())
                .messageId(report.getMessageId())
                .reportedByUserId(report.getReportedBy().getId())
                .reportedByUserName(report.getReportedBy().getUserName())
                .messageContent("") // TODO: Fetch from MongoDB
                .channelName("")
                .serverName("")
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .reviewedByAdminId(report.getReviewedByAdmin() != null ? report.getReviewedByAdmin().getId() : null)
                .reviewedByAdminName(report.getReviewedByAdmin() != null ? report.getReviewedByAdmin().getUserName() : null)
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void resolveReport(Long reportId, String action, Long adminId, String ipAddress) {
        ReportedMessage report = reportedMessageRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(ReportedMessage.ReportStatus.RESOLVED);
        report.setReviewedAt(LocalDateTime.now());
        // TODO: Set reviewedByAdmin
        reportedMessageRepository.save(report);

        // Log audit
        AuditLog log = AuditLog.builder()
                .admin(userRepository.getReferenceById(adminId))
                .action("RESOLVE_REPORT")
                .targetType("REPORT")
                .targetId(reportId)
                .details("{\"action\": \"" + action + "\"}")
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(log);

        // Handle action
        switch (action) {
            case "DELETE_MESSAGE" -> deleteMessage(report.getMessageId());
            case "BAN_USER" -> banUserPermanently(report.getReportedBy().getId(), "Banned due to reported message", adminId);
            case "WARN_USER" -> warnUser(report.getReportedBy().getId(), "Warning from report: " + report.getReason(), adminId);
        }
    }

    @Override
    @Transactional
    public void deleteReport(Long reportId) {
        reportedMessageRepository.deleteById(reportId);
    }

    // ===== MODERATION ACTIONS =====

    @Override
    @Transactional
    public void deleteMessage(String messageId) {
        // TODO: Delete from MongoDB (ChannelMessage or DirectMessage)
    }

    @Override
    @Transactional
    public void warnUser(Long userId, String reason, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        warningRepository.save(Warning.builder()
                .user(user)
                .admin(userRepository.getReferenceById(adminId))
                .reason(reason)
                .build());

        // Log audit
        auditLogRepository.save(AuditLog.builder()
                .admin(userRepository.getReferenceById(adminId))
                .action("WARN_USER")
                .targetType("USER")
                .targetId(userId)
                .details("{\"reason\": \"" + reason + "\"}")
                .build());
    }

    @Override
    @Transactional
    public void banUserPermanently(Long userId, String reason, Long adminId) {
        banUser(userId, reason);
    }

    // ===== BLACKLIST MANAGEMENT =====

    @Override
    public List<BlacklistKeywordResponse> getBlacklist() {
        return autoModBlacklistRepository.findAll().stream()
                .map(bl -> BlacklistKeywordResponse.builder()
                        .id(bl.getId())
                        .keyword(bl.getKeyword())
                        .createdByAdminId(bl.getCreatedByAdmin().getId())
                        .createdByAdminName(bl.getCreatedByAdmin().getUserName())
                        .createdAt(bl.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public BlacklistKeywordResponse addBlacklistKeyword(String keyword, Long adminId) {
        if (autoModBlacklistRepository.existsByKeyword(keyword)) {
            throw new IllegalArgumentException("Keyword already exists in blacklist");
        }

        AutoModBlacklist blacklist = AutoModBlacklist.builder()
                .keyword(keyword)
                .createdByAdmin(userRepository.getReferenceById(adminId))
                .build();

        blacklist = autoModBlacklistRepository.save(blacklist);

        return BlacklistKeywordResponse.builder()
                .id(blacklist.getId())
                .keyword(blacklist.getKeyword())
                .createdByAdminId(adminId)
                .createdByAdminName("")
                .createdAt(blacklist.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void removeBlacklistKeyword(Long blacklistId) {
        autoModBlacklistRepository.deleteById(blacklistId);
    }

    @Override
    public boolean isMessageContainsBlacklistedWord(String content) {
        return autoModBlacklistRepository.findAll().stream()
                .anyMatch(bl -> content.toLowerCase().contains(bl.getKeyword().toLowerCase()));
    }

    // ===== AUDIT LOGS =====

    @Override
    public Page<AuditLogResponse> getAuditLogs(Specification<AuditLog> spec, Pageable pageable) {
        // TODO: Implement with proper projection
        return Page.empty();
    }

    @Override
    @Transactional
    public void logAudit(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }
}
