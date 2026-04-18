package com.discordclone.backend.Controller.api.admin;

import com.discordclone.backend.dto.request.AdminLoginRequest;
import com.discordclone.backend.dto.request.BlacklistKeywordRequest;
import com.discordclone.backend.dto.request.BulkUserActionRequest;
import com.discordclone.backend.dto.request.ReportResolveRequest;
import com.discordclone.backend.dto.response.*;
import com.discordclone.backend.entity.jpa.ReportedMessage;
import com.discordclone.backend.entity.jpa.Server;
import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.security.jwt.JwtUtils;
import com.discordclone.backend.security.services.UserDetailsImpl;
import com.discordclone.backend.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final JwtUtils jwtUtils;

    // ===== AUTHENTICATION =====

    // Login is handled in AdminAuthController

    // ===== DASHBOARD STATS =====

    @GetMapping("/stats/overview")
    public ResponseEntity<AdminStatsOverview> getOverviewStats() {
        return ResponseEntity.ok(adminService.getOverviewStats());
    }

    @GetMapping("/stats/user-growth")
    public ResponseEntity<List<UserGrowthData>> getUserGrowth(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        java.time.LocalDate fromDate = (from != null && !from.isEmpty()) ? java.time.LocalDate.parse(from) : java.time.LocalDate.now().minusDays(30);
        java.time.LocalDate toDate = (to != null && !to.isEmpty()) ? java.time.LocalDate.parse(to) : java.time.LocalDate.now();
        return ResponseEntity.ok(adminService.getUserGrowth(fromDate, toDate));
    }

    @GetMapping("/stats/user-retention")
    public ResponseEntity<?> getUserRetention() {
        // TODO: Implement
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/stats/top-servers")
    public ResponseEntity<List<AdminServerSummary>> getTopServers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminService.getTopServers(limit));
    }

    @GetMapping("/stats/engagement")
    public ResponseEntity<?> getEngagementStats() {
        // TODO: Implement
        return ResponseEntity.ok(List.of());
    }

    // ===== USER MANAGEMENT =====

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserSummary>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("userName")), "%" + search.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("displayName")), "%" + search.toLowerCase() + "%")
                    ));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), active));
        }
        return ResponseEntity.ok(adminService.getAllUsers(spec, pageable));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long userId) {
        // TODO: Implement detailed user view
        return ResponseEntity.ok("User detail");
    }

    @PutMapping("/users/{userId}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long userId) {
        adminService.disableUser(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long userId) {
        adminService.enableUser(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long userId, @RequestParam String reason) {
        adminService.banUser(userId, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long userId) {
        adminService.unbanUser(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/bulk-disable")
    public ResponseEntity<Void> bulkDisableUsers(@Valid @RequestBody BulkUserActionRequest request) {
        adminService.bulkDisableUsers(request.getUserIds(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/bulk-ban")
    public ResponseEntity<Void> bulkBanUsers(@Valid @RequestBody BulkUserActionRequest request) {
        adminService.bulkBanUsers(request.getUserIds(), request.getReason());
        return ResponseEntity.ok().build();
    }

    // ===== SERVER MANAGEMENT =====

    @GetMapping("/servers")
    public ResponseEntity<Page<AdminServerSummary>> getAllServers(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Specification<Server> spec = Specification.where(null);
        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
        }
        return ResponseEntity.ok(adminService.getAllServers(spec, pageable));
    }

    @GetMapping("/servers/{serverId}")
    public ResponseEntity<?> getServerDetail(@PathVariable Long serverId) {
        // TODO: Implement
        return ResponseEntity.ok("Server detail");
    }

    @DeleteMapping("/servers/{serverId}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long serverId) {
        adminService.deleteServer(serverId);
        return ResponseEntity.ok().build();
    }

    // ===== REPORTED MESSAGES =====

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportedMessageResponse>> getAllReports(
            @RequestParam(required = false) ReportedMessage.ReportStatus status,
            Pageable pageable) {
        Specification<ReportedMessage> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return ResponseEntity.ok(adminService.getAllReports(spec, pageable));
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ReportedMessageResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminService.getReportById(reportId));
    }

    @PutMapping("/reports/{reportId}/resolve")
    public ResponseEntity<Void> resolveReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportResolveRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        adminService.resolveReport(reportId, request.getAction(), userDetails.getId(), "127.0.0.1");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long reportId) {
        adminService.deleteReport(reportId);
        return ResponseEntity.ok().build();
    }

    // ===== MODERATION ACTIONS =====

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId) {
        adminService.deleteMessage(messageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/warn")
    public ResponseEntity<Void> warnUser(
            @PathVariable Long userId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        adminService.warnUser(userId, reason, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/ban-permanent")
    public ResponseEntity<Void> banUserPermanent(
            @PathVariable Long userId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        adminService.banUserPermanently(userId, reason, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    // ===== AUTO-MODERATION BLACKLIST =====

    @GetMapping("/moderation/blacklist")
    public ResponseEntity<List<BlacklistKeywordResponse>> getBlacklist() {
        return ResponseEntity.ok(adminService.getBlacklist());
    }

    @PostMapping("/moderation/blacklist")
    public ResponseEntity<BlacklistKeywordResponse> addBlacklistKeyword(
            @Valid @RequestBody BlacklistKeywordRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(adminService.addBlacklistKeyword(request.getKeyword(), userDetails.getId()));
    }

    @DeleteMapping("/moderation/blacklist/{blacklistId}")
    public ResponseEntity<Void> removeBlacklistKeyword(@PathVariable Long blacklistId) {
        adminService.removeBlacklistKeyword(blacklistId);
        return ResponseEntity.ok().build();
    }

    // ===== AUDIT LOGS =====

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long adminId,
            Pageable pageable) {
        // TODO: Implement specification
        return ResponseEntity.ok(Page.empty());
    }

    @GetMapping("/audit-logs/{logId}")
    public ResponseEntity<?> getAuditLogDetail(@PathVariable Long logId) {
        // TODO: Implement
        return ResponseEntity.ok("Audit log detail");
    }

    // ===== NITRO PAYMENT ADMIN =====

    @GetMapping("/payment/orders")
    public ResponseEntity<?> getAllNitroOrders(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        // TODO: Implement - extend NitroPaymentController
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/payment/orders/{txnRef}")
    public ResponseEntity<?> getNitroOrderDetail(@PathVariable String txnRef) {
        // TODO: Implement
        return ResponseEntity.ok("Order detail");
    }

    @PutMapping("/payment/orders/{txnRef}/approve")
    public ResponseEntity<Void> approveOrder(@PathVariable String txnRef) {
        // TODO: Implement
        return ResponseEntity.ok().build();
    }

    @PutMapping("/payment/orders/{txnRef}/reject")
    public ResponseEntity<Void> rejectOrder(@PathVariable String txnRef) {
        // TODO: Implement
        return ResponseEntity.ok().build();
    }
}
