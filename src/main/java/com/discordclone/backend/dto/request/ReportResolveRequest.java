package com.discordclone.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResolveRequest {
    @NotBlank
    private String action; // "DELETE_MESSAGE", "WARN_USER", "BAN_USER", "DISMISS"
}
