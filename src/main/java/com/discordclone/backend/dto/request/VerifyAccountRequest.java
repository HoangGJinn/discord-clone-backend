package com.discordclone.backend.dto.request;

import lombok.Data;

@Data
public class VerifyAccountRequest {
    private String email;
    private String otp;
}
