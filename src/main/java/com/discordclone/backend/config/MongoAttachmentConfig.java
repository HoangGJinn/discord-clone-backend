package com.discordclone.backend.config;

import com.discordclone.backend.dto.message.MessageAttachment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoAttachmentConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new StringToMessageAttachmentConverter()));
    }

    @ReadingConverter
    static class StringToMessageAttachmentConverter implements Converter<String, MessageAttachment> {
        @Override
        public MessageAttachment convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }

            return MessageAttachment.builder()
                    .url(source)
                    .build();
        }
    }
}
