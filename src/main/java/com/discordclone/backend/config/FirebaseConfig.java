package com.discordclone.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Khởi tạo Firebase Admin SDK một lần khi app start.
 *
 * Hỗ trợ 2 cách cấu hình (ưu tiên theo thứ tự):
 *  1. Biến môi trường FIREBASE_SERVICE_ACCOUNT_JSON (JSON string) — dùng cho deploy
 *  2. File classpath firebase-service-account.json                 — dùng cho local dev
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    // JSON string của service account — set làm env var trên Render/Railway
    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    // Đường dẫn file — dùng khi dev local đặt file trong resources/
    @Value("${firebase.service-account-file:}")
    private String serviceAccountFile;

    @PostConstruct
    public void initFirebase() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                GoogleCredentials credentials = resolveCredentials();
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully.");
            } catch (IOException e) {
                log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage(), e);
                throw new RuntimeException("Firebase initialization failed", e);
            }
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        // Ưu tiên 1: JSON string từ env var (dùng trên server deploy)
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            log.info("Loading Firebase credentials from env variable FIREBASE_SERVICE_ACCOUNT_JSON");
            InputStream stream = new ByteArrayInputStream(
                    serviceAccountJson.getBytes(StandardCharsets.UTF_8));
            return GoogleCredentials.fromStream(stream);
        }

        // Ưu tiên 2: File path (dùng khi dev local)
        if (serviceAccountFile != null && !serviceAccountFile.isBlank()) {
            log.info("Loading Firebase credentials from file: {}", serviceAccountFile);
            return GoogleCredentials.fromStream(new FileInputStream(serviceAccountFile));
        }

        // Fallback: Application Default Credentials
        log.warn("No Firebase credentials configured. Falling back to Application Default Credentials.");
        return GoogleCredentials.getApplicationDefault();
    }
}
