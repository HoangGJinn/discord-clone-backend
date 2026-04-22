package com.discordclone.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsOverview {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalServers;
    private Long totalMessages;
    private Long totalRevenue;
    private Long dailyActiveUsersToday;
    private Long newUsersToday;
    private Long totalReports;
    private Long pendingReports;

    // Growth percentages (e.g. +12.5)
    private Double userGrowth;
    private Double serverGrowth;
    private Double messageGrowth;
    private Double revenueGrowth;
}
