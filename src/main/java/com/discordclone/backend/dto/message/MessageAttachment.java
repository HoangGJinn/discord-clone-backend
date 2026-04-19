package com.discordclone.backend.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {
    private String url;
    private String filename;
    private String contentType;
    private Long size;
}
