package com.discordclone.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint với SockJS cho web browser
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép mọi domain CORS
                .withSockJS(); // Hỗ trợ fallback nếu trình duyệt không có websocket

        // Endpoint WebSocket thuần cho React Native (không dùng SockJS)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix cho các tin nhắn từ server gửi xuống client
        registry.enableSimpleBroker("/topic");

        // Prefix cho các tin nhắn từ client gửi lên server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix cho tin nhắn riêng tư tới user cụ thể
        registry.setUserDestinationPrefix("/user");
    }
}
