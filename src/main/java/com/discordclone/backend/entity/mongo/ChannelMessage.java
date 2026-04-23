package com.discordclone.backend.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.discordclone.backend.dto.message.MessageAttachment;

@Document(collection = "channel_messages")
@CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMessage {
    @Id
    private String id;
    @Indexed
    private Long channelId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String senderAvatarEffectId;
    private String senderBannerEffectId;
    private String senderCardEffectId;
    private String content;
    private String replyToId;

    @Builder.Default
    @lombok.Setter(lombok.AccessLevel.NONE)
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

    private Boolean edited = false;
    private Boolean deleted = false;
    private Boolean pinned = false;
    private Date createdAt = new Date();
    private Date updatedAt = new Date();

    @Builder.Default
    private List<Reaction> reactions = new ArrayList<>();

    // XÓA Getter/Setter Reactions thủ công đi vì nó đang gây lỗi instantiate

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reaction {
        private Long userId;
        private String emoji;
    }
}
