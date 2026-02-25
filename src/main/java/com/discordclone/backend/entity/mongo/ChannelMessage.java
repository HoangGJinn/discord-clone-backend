package com.discordclone.backend.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "channel_messages")
public class ChannelMessage {
    @Id
    private String id;

    private Long channelId;
    private Long userId; // Sender ID
    private String content;

    private Date createdAt;
    private Date updatedAt;

    @Builder.Default
    private boolean edited = false;

    @Builder.Default
    private boolean deleted = false;
}
