package com.discordclone.backend.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "conversations")
@CompoundIndex(name = "uk_user_pair", def = "{'user1Id': 1, 'user2Id': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    private String id;

    private Long user1Id;
    private Long user2Id;

    private Date createdAt;
    private Date updatedAt;
    private Date user1LastReadAt;
    private Date user2LastReadAt;
}
