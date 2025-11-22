package com.inhacapstone04.embersentinelserver.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class FcmConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            // 방법 1: resources/firebase-service-account.json 파일 사용 (개발용)
            // InputStream serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();

            // 방법 2: 환경 변수 (GOOGLE_APPLICATION_CREDENTIALS) 사용 (배포용/권장)
            // Docker/EC2 배포 시 환경 변수로 경로를 지정하면 자동으로 읽어옵니다.

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();

            log.info("FirebaseApp Initialized");
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
