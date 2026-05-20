package com.inhacapstone04.embersentinelserver.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 로컬 개발 환경용 FCM 설정.
 * fcm.enabled=false 일 때 활성화되며, FirebaseMessaging 빈을 등록하지 않는다.
 * FcmService에서 @Autowired(required=false)로 null 주입을 허용한다.
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "false")
public class FcmLocalConfig {

    @PostConstruct
    public void init() {
        log.warn("=== FCM 비활성화 상태 (로컬 환경) — 푸시 알림이 전송되지 않습니다 ===");
    }
}
