package com.discordclone.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username không được để trống")
    String userName;

    @NotBlank(message = "Password không được để trống")
    String password;
}
