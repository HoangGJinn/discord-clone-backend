package com.discordclone.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditMessageRequest {

    @NotBlank(message = "Content is required")
    private String content;
}
