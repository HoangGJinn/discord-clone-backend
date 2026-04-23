package com.discordclone.backend.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

import com.discordclone.backend.dto.message.MessageAttachment;

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
    @Setter(AccessLevel.NONE)
    private List<Object> attachments = new ArrayList<>();

    public List<MessageAttachment> getAttachments() {
        if (attachments == null) {
            return new ArrayList<>();
        }

        List<MessageAttachment> normalized = new ArrayList<>();
        for (Object item : attachments) {
            if (item == null) {
                continue;
            }

            if (item instanceof MessageAttachment attachment) {
                if (attachment.getUrl() != null && !attachment.getUrl().isBlank()) {
                    normalized.add(attachment);
                }
                continue;
            }

            if (item instanceof String legacyUrl) {
                if (!legacyUrl.isBlank()) {
                    normalized.add(MessageAttachment.builder().url(legacyUrl).build());
                }
                continue;
            }

            if (item instanceof Map<?, ?> map) {
                Object urlValue = map.get("url");
                if (urlValue == null) {
                    continue;
                }
                String url = String.valueOf(urlValue);
                if (url.isBlank()) {
                    continue;
                }

                MessageAttachment attachment = MessageAttachment.builder()
                        .url(url)
                        .filename(map.get("filename") != null ? String.valueOf(map.get("filename")) : null)
                        .contentType(map.get("contentType") != null ? String.valueOf(map.get("contentType")) : null)
                        .size(parseSize(map.get("size")))
                        .build();
                normalized.add(attachment);
            }
        }

        return normalized;
    }

    public void setAttachments(List<?> attachments) {
        this.attachments = attachments == null ? new ArrayList<>() : new ArrayList<>(attachments);
    }

    private Long parseSize(Object rawSize) {
        if (rawSize == null) {
            return null;
        }

        if (rawSize instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(rawSize));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

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
