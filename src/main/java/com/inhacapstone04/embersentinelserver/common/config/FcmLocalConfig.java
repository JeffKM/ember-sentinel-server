package com.inhacapstone04.embersentinelserver.common.config;

import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 로컬 개발 환경용 FCM 설정.
 * fcm.enabled=false 일 때 활성화되며, FirebaseMessaging 빈 대신
 * null을 반환하여 FCM 의존성 없이 서버가 기동되도록 한다.
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "false")
public class FcmLocalConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        log.warn("=== FCM 비활성화 상태 (로컬 환경) — 푸시 알림이 전송되지 않습니다 ===");
        return null;
    }
}
