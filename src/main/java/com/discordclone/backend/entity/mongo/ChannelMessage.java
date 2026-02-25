package com.discordclone.backend.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "channel_messages")
@CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}")
@Data
public class ChannelMessage {

    @Id
    private String id;

    @Indexed
    private Long channelId;

    private Long senderId;
    private String senderName;
    private String senderAvatar;

    private String content;
    private List<String> attachments = new ArrayList<>();

    private Boolean edited = false;
    private Boolean deleted = false;
    private Boolean pinned = false;

    private Date createdAt = new Date();
    private Date updatedAt = new Date();

    private List<Reaction> reactions = new ArrayList<>();

    public List<Reaction> getReactions() {
        return new ArrayList<>(reactions);
    }

    public void setReactions(List<Reaction> reactions) {
        this.reactions = reactions == null ? new ArrayList<>() : new ArrayList<>(reactions);
    }

    @Data
    public static class Reaction {
        private Long userId;
        private String emoji;
    }
}
