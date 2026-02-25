package com.discordclone.backend.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "direct_messages")
public class DirectMessage {
    @Id
    private String id;
    private String conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;

    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    private String replyToId;

    @Builder.Default
    private boolean isRead = false;

    private Date createdAt;
    private Date updatedAt;

    private boolean edited;
    private boolean deleted;

    @Builder.Default
    private Map<String, Set<Long>> reactions = new HashMap<>();
}
