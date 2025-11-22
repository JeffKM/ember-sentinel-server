package com.inhacapstone04.embersentinelserver.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FcmConfig {

    @Value("${fcm.key-path:}")
    private String fcmKeyPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials;

        // 1. application.yml에 설정된 경로가 있는지 확인
        if (fcmKeyPath != null && !fcmKeyPath.isEmpty()) {
            try {
                // Classpath(resources 폴더)에서 먼저 찾아봄
                ClassPathResource resource = new ClassPathResource(fcmKeyPath);
                if (resource.exists()) {
                    log.info("Loading FCM credentials from classpath: {}", fcmKeyPath);
                    try (InputStream is = resource.getInputStream()) {
                        credentials = GoogleCredentials.fromStream(is);
                    }
                } else {
                    // Classpath에 없으면 절대 경로(파일 시스템)로 시도
                    log.info("Loading FCM credentials from file system: {}", fcmKeyPath);
                    try (InputStream is = new FileInputStream(fcmKeyPath)) {
                        credentials = GoogleCredentials.fromStream(is);
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to load FCM key from path: {}. Falling back to default credentials.", fcmKeyPath);
                credentials = GoogleCredentials.getApplicationDefault();
            }
        } else {
            // 2. 설정된 경로가 없으면 환경 변수(GOOGLE_APPLICATION_CREDENTIALS) 기반 기본 자격 증명 사용
            log.info("No FCM key path configured. Using Application Default Credentials.");
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        log.info("FirebaseApp Initialized");
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
