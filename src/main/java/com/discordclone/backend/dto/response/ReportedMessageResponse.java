package com.discordclone.backend.dto.response;

import com.discordclone.backend.entity.jpa.ReportedMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportedMessageResponse {
    private Long reportId;
    private String messageId;
    private Long reportedByUserId;
    private String reportedByUserName;
    private String messageContent;
    private String channelName;
    private String serverName;
    private ReportedMessage.ReportReason reason;
    private String description;
    private ReportedMessage.ReportStatus status;
    private Long reviewedByAdminId;
    private String reviewedByAdminName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
