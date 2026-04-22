package com.discordclone.backend.service.impl;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service gửi FCM notification trực tiếp từ Spring Boot.
 *
 * Payload conventions (React Native client sẽ parse field "type"):
 *  - "server_message" → navigate đến channel
 *  - "dm"            → navigate đến DM conversation
 *  - "call_invite"   → hiển thị IncomingCall popup (data-only, priority HIGH)
 *  - "friend_request"→ hiển thị notification kết bạn
 *
 * Tất cả giá trị trong data map phải là String (FCM yêu cầu).
 */
@Service
@Slf4j
public class FcmService {

    private static final int BATCH_SIZE = 500;

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    /** Thông báo tin nhắn mới trong channel của server */
    @Async
    public void sendServerMessageNotification(
            List<String> recipientTokens,
            String senderName,
            String channelName,
            String serverId,
            String channelId,
            String content) {

        if (isEmpty(recipientTokens)) return;

        String title = senderName + " trong #" + channelName;
        String body  = truncate(content, 180, "Bạn có tin nhắn mới");

        sendBatched("server_message[ch=" + channelId + "]", recipientTokens, batch ->
                MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(notification(title, body))
                        .setAndroidConfig(androidHighPriority("chat_messages"))
                        .putAllData(Map.of(
                                "type",        "server_message",
                                "serverId",    serverId,
                                "channelId",   channelId,
                                "channelName", channelName,
                                "senderName",  senderName,
                                "title",       title,
                                "body",        body
                        ))
                        .build()
        );
    }

    /** Thông báo tin nhắn trực tiếp (DM) */
    @Async
    public void sendDmNotification(
            List<String> receiverTokens,
            String senderName,
            String senderId,
            String conversationId,
            String content) {

        if (isEmpty(receiverTokens)) return;

        String title = senderName;
        String body  = truncate(content, 180, "Bạn có tin nhắn mới");

        sendBatched("dm[conv=" + conversationId + "]", receiverTokens, batch ->
                MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(notification(title, body))
                        .setAndroidConfig(androidHighPriority("chat_messages"))
                        .putAllData(Map.of(
                                "type",           "dm",
                                "conversationId", conversationId,
                                "senderId",       senderId,
                                "senderName",     senderName,
                                "title",          title,
                                "body",           body
                        ))
                        .build()
        );
    }

    /**
     * Thông báo cuộc gọi đến — data-only, priority HIGH.
     *
     * Không có "notification" block → Android giao toàn quyền xử lý cho app.
     * Client dùng expo-notifications background handler hoặc
     * @react-native-firebase/messaging onBackgroundMessage để hiển thị
     * màn hình cuộc gọi đến (IncomingCall popup / CallKit).
     */
    @Async
    public void sendCallInviteNotification(
            List<String> calleeTokens,
            String callerName,
            String callerId,
            String calleeId,
            String conversationId,
            String callType) {         // "VOICE" | "VIDEO"

        if (isEmpty(calleeTokens)) return;

        String normalizedType = callType == null ? "VOICE" : callType.toUpperCase();
        String body = "VIDEO".equals(normalizedType) ? "Cuộc gọi video đến" : "Cuộc gọi thoại đến";

        sendBatched("call_invite[conv=" + conversationId + "]", calleeTokens, batch ->
                MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(notification(callerName, body))
                        .setAndroidConfig(androidHighPriority("chat_messages"))
                        .putAllData(Map.of(
                                "type",           "call_invite",
                                "categoryId",     "call_invite_category",
                                "conversationId", conversationId,
                                "callerId",       callerId,
                                "callerName",     callerName,
                                "calleeId",       calleeId,
                                "callType",       normalizedType,
                                "title",          callerName,
                                "body",           body
                        ))
                        .build()
        );
    }

    /** Thông báo lời mời kết bạn */
    @Async
    public void sendFriendRequestNotification(
            List<String> receiverTokens,
            String senderName,
            String senderId,
            String friendshipId) {

        if (isEmpty(receiverTokens)) return;

        String title = "Lời mời kết bạn";
        String body  = senderName + " đã gửi cho bạn một lời mời kết bạn";

        sendBatched("friend_request[from=" + senderId + "]", receiverTokens, batch ->
                MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(notification(title, body))
                        .setAndroidConfig(androidHighPriority("social_notifications"))
                        .putAllData(Map.of(
                                "type",         "friend_request",
                                "senderId",     senderId,
                                "senderName",   senderName,
                                "friendshipId", friendshipId,
                                "title",        title,
                                "body",         body
                        ))
                        .build()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ═══════════════════════════════════════════════════════════════

    @FunctionalInterface
    private interface BatchMessageBuilder {
        MulticastMessage build(List<String> tokenBatch);
    }

    private void sendBatched(String logTag, List<String> tokens, BatchMessageBuilder builder) {
        for (List<String> batch : partition(tokens, BATCH_SIZE)) {
            try {
                BatchResponse response = FirebaseMessaging.getInstance()
                        .sendEachForMulticast(builder.build(batch));

                log.info("[FCM][{}] delivered={}/{}", logTag, response.getSuccessCount(), batch.size());

                // Log chi tiết token thất bại
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        FirebaseMessagingException ex = responses.get(i).getException();
                        log.warn("[FCM][{}] token[{}] failed: {}",
                                logTag, i,
                                ex != null ? ex.getMessagingErrorCode() : "unknown");
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FCM][{}] batch error: {}", logTag, e.getMessage());
            }
        }
    }

    private static Notification notification(String title, String body) {
        return Notification.builder().setTitle(title).setBody(body).build();
    }

    private static AndroidConfig androidHighPriority(String channelId) {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setChannelId(channelId)
                        .build())
                .build();
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        return parts;
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private static String truncate(String text, int maxLen, String fallback) {
        if (text == null || text.isBlank()) return fallback;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
