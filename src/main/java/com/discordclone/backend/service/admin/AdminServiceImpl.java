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
import java.util.Map;
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

        // Calculate growth (last 30 days vs previous 30 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);

        Long usersLast30 = userRepository.countByCreatedAtBetween(thirtyDaysAgo, now);
        Long usersPrev30 = userRepository.countByCreatedAtBetween(sixtyDaysAgo, thirtyDaysAgo);
        Double userGrowth = calculateGrowth(usersLast30, usersPrev30);

        Long serversLast30 = serverRepository.countByCreatedAtBetween(thirtyDaysAgo, now);
        Long serversPrev30 = serverRepository.countByCreatedAtBetween(sixtyDaysAgo, thirtyDaysAgo);
        Double serverGrowth = calculateGrowth(serversLast30, serversPrev30);

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
                .userGrowth(userGrowth)
                .serverGrowth(serverGrowth)
                .messageGrowth(0.0)
                .revenueGrowth(0.0)
                .build();
    }

    private Double calculateGrowth(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100.0;
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
                        .isBanned(server.getIsBanned() != null ? server.getIsBanned() : false)
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
    public void disableUser(Long userId, Long adminId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);

        logAudit(adminId, "DISABLE_USER", "USER", userId, "{\"userName\": \"" + user.getUserName() + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void enableUser(Long userId, Long adminId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);

        logAudit(adminId, "ENABLE_USER", "USER", userId, "{\"userName\": \"" + user.getUserName() + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void banUser(Long userId, String reason, Long adminId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);

        logAudit(adminId, "BAN_USER", "USER", userId, "{\"reason\": \"" + reason + "\", \"userName\": \"" + user.getUserName() + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId, Long adminId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);

        logAudit(adminId, "UNBAN_USER", "USER", userId, "{\"userName\": \"" + user.getUserName() + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void bulkDisableUsers(List<Long> userIds, String reason, Long adminId, String ipAddress) {
        List<User> users = userRepository.findAllById(userIds);
        users.forEach(user -> user.setIsActive(false));
        userRepository.saveAll(users);

        logAudit(adminId, "BULK_DISABLE_USERS", "USER", null, "{\"userIds\": " + userIds + ", \"reason\": \"" + reason + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void bulkBanUsers(List<Long> userIds, String reason, Long adminId, String ipAddress) {
        List<User> users = userRepository.findAllById(userIds);
        users.forEach(user -> user.setIsActive(false));
        userRepository.saveAll(users);

        logAudit(adminId, "BULK_BAN_USERS", "USER", null, "{\"userIds\": " + userIds + ", \"reason\": \"" + reason + "\"}", ipAddress);
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
                        .isBanned(server.getIsBanned() != null ? server.getIsBanned() : false)
                        .build())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), summaries.size());
        List<AdminServerSummary> pageContent = start <= end ? summaries.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, summaries.size());
    }

    @Override
    @Transactional
    public void deleteServer(Long serverId, Long adminId, String ipAddress) {
        Server server = serverRepository.findByIdWithMembers(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server not found"));
        String serverName = server.getName();
        serverRepository.delete(server);

        logAudit(adminId, "DELETE_SERVER", "SERVER", serverId, "{\"serverName\": \"" + serverName + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void banServer(Long serverId, String reason, Long adminId, String ipAddress) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server not found"));
        server.setIsBanned(true);
        serverRepository.save(server);

        logAudit(adminId, "BAN_SERVER", "SERVER", serverId, "{\"reason\": \"" + reason + "\", \"serverName\": \"" + server.getName() + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void unbanServer(Long serverId, Long adminId, String ipAddress) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server not found"));
        server.setIsBanned(false);
        serverRepository.save(server);

        logAudit(adminId, "UNBAN_SERVER", "SERVER", serverId, "{\"serverName\": \"" + server.getName() + "\"}", ipAddress);
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
            case "BAN_USER" -> banUserPermanently(report.getReportedBy().getId(), "Banned due to reported message", adminId, ipAddress);
            case "WARN_USER" -> warnUser(report.getReportedBy().getId(), "Warning from report: " + report.getReason(), adminId, ipAddress);
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
    public void warnUser(Long userId, String reason, Long adminId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        warningRepository.save(Warning.builder()
                .user(user)
                .admin(userRepository.getReferenceById(adminId))
                .reason(reason)
                .build());

        logAudit(adminId, "WARN_USER", "USER", userId, "{\"reason\": \"" + reason + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void banUserPermanently(Long userId, String reason, Long adminId, String ipAddress) {
        banUser(userId, reason, adminId, ipAddress);
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
    public BlacklistKeywordResponse addBlacklistKeyword(String keyword, Long adminId, String ipAddress) {
        if (autoModBlacklistRepository.existsByKeyword(keyword)) {
            throw new IllegalArgumentException("Keyword already exists in blacklist");
        }

        AutoModBlacklist blacklist = AutoModBlacklist.builder()
                .keyword(keyword)
                .createdByAdmin(userRepository.getReferenceById(adminId))
                .build();
        
        AutoModBlacklist saved = autoModBlacklistRepository.save(blacklist);

        logAudit(adminId, "ADD_BLACKLIST", "SYSTEM", saved.getId(), "{\"keyword\": \"" + keyword + "\"}", ipAddress);

        return BlacklistKeywordResponse.builder()
                .id(saved.getId())
                .keyword(saved.getKeyword())
                .createdByAdminId(saved.getCreatedByAdmin().getId())
                .createdByAdminName(saved.getCreatedByAdmin().getUserName())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void removeBlacklistKeyword(Long blacklistId, Long adminId, String ipAddress) {
        AutoModBlacklist blacklist = autoModBlacklistRepository.findById(blacklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Keyword not found"));
        String keyword = blacklist.getKeyword();
        autoModBlacklistRepository.delete(blacklist);

        logAudit(adminId, "REMOVE_BLACKLIST", "SYSTEM", blacklistId, "{\"keyword\": \"" + keyword + "\"}", ipAddress);
    }

    @Override
    public boolean isMessageContainsBlacklistedWord(String content) {
        return autoModBlacklistRepository.findAll().stream()
                .anyMatch(bl -> content.toLowerCase().contains(bl.getKeyword().toLowerCase()));
    }

    // ===== AUDIT LOGS =====

    @Override
    public Page<AuditLogResponse> getAuditLogs(Specification<AuditLog> spec, Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findAll(spec, pageable);
        return auditLogs.map(log -> AuditLogResponse.builder()
                .id(log.getId())
                .adminId(log.getAdmin().getId())
                .adminName(log.getAdmin().getUserName())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build());
    }

    @Override
    public AuditLogResponse getAuditLogById(Long logId) {
        AuditLog log = auditLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found"));
        return AuditLogResponse.builder()
                .id(log.getId())
                .adminId(log.getAdmin().getId())
                .adminName(log.getAdmin().getUserName())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void logAudit(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    private void logAudit(Long adminId, String action, String targetType, Long targetId, String details, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .admin(userRepository.getReferenceById(adminId))
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(log);
    }

    // ===== NITRO PAYMENT ADMIN =====

    @Override
    public Page<NitroOrderSummary> getAllOrders(Specification<NitroOrder> spec, Pageable pageable) {
        Page<NitroOrder> orders = nitroOrderRepository.findAll(spec, pageable);
        return orders.map(order -> {
            String userName = "";
            if (order.getUserId() != null) {
                Optional<User> user = userRepository.findById(order.getUserId());
                userName = user.map(User::getUserName).orElse("Unknown");
            }
            return NitroOrderSummary.builder()
                    .id(order.getId())
                    .txnRef(order.getVnpTxnRef())
                    .userId(order.getUserId())
                    .userName(userName)
                    .amount(order.getAmount() != null ? order.getAmount().intValue() : 0)
                    .status(order.getStatus())
                    .paymentMethod("VNPay")
                    .createdAt(order.getCreatedAt())
                    .build();
        });
    }

    @Override
    public NitroOrderSummary getOrderByTxnRef(String txnRef) {
        NitroOrder order = nitroOrderRepository.findByVnpTxnRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with txnRef: " + txnRef));
        String userName = "";
        if (order.getUserId() != null) {
            Optional<User> user = userRepository.findById(order.getUserId());
            userName = user.map(User::getUserName).orElse("Unknown");
        }
        return NitroOrderSummary.builder()
                .id(order.getId())
                .txnRef(order.getVnpTxnRef())
                .userId(order.getUserId())
                .userName(userName)
                .amount(order.getAmount() != null ? order.getAmount().intValue() : 0)
                .status(order.getStatus())
                .paymentMethod("VNPay")
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void approveOrder(String txnRef, Long adminId, String ipAddress) {
        NitroOrder order = nitroOrderRepository.findByVnpTxnRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with txnRef: " + txnRef));
        order.setStatus("CONFIRMED");
        nitroOrderRepository.save(order);

        logAudit(adminId, "APPROVE_NITRO", "ORDER", order.getId(), "{\"txnRef\": \"" + txnRef + "\"}", ipAddress);
    }

    @Override
    @Transactional
    public void rejectOrder(String txnRef, Long adminId, String ipAddress) {
        NitroOrder order = nitroOrderRepository.findByVnpTxnRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with txnRef: " + txnRef));
        order.setStatus("FAILED");
        nitroOrderRepository.save(order);

        logAudit(adminId, "REJECT_NITRO", "ORDER", order.getId(), "{\"txnRef\": \"" + txnRef + "\"}", ipAddress);
    }

    @Override
    public Map<String, Object> getRevenueStats() {
        Long totalRevenue = nitroOrderRepository.sumConfirmedRevenue();
        Long totalOrders = nitroOrderRepository.count();
        Long successCount = nitroOrderRepository.countByStatus("CONFIRMED");
        Long pendingCount = nitroOrderRepository.countByStatus("PENDING");
        Long failedCount = nitroOrderRepository.countByStatus("FAILED");
        double successRate = totalOrders > 0 ? (successCount * 100.0 / totalOrders) : 0;

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", totalOrders);
        stats.put("successRate", Math.round(successRate * 10.0) / 10.0);
        stats.put("successCount", successCount);
        stats.put("pendingCount", pendingCount);
        stats.put("failedCount", failedCount);
        return stats;
    }
}
