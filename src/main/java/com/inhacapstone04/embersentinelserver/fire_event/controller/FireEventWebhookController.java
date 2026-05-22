package com.inhacapstone04.embersentinelserver.fire_event.controller;

import com.inhacapstone04.embersentinelserver.common.service.LiveKitWebhookEventSeperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(name = "livekit.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class FireEventWebhookController {

    private final LiveKitWebhookEventSeperationService liveKitWebhookEventSeperationService;

    /**
     * LiveKit 서버로부터 Webhook 이벤트를 수신합니다.
     * 단일 엔드포인트에서 모든 이벤트를 받아 Service Layer로 위임합니다.
     */
    @PostMapping(value = "/livekit/webhook", consumes = "application/webhook+json")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String body,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // Authorization 헤더가 없는 경우 방어
        if (authHeader == null || authHeader.isEmpty()) {
            log.warn("Webhook received without Authorization header");
            return ResponseEntity.badRequest().body("Missing Authorization Header");
        }

        // 비즈니스 로직은 Service로 위임
        liveKitWebhookEventSeperationService.handleWebhookEvent(body, authHeader);

        // LiveKit 서버에 200 OK 응답 (처리가 비동기거나 실패하더라도 재전송 방지를 위해 OK 반환 권장)
        return ResponseEntity.ok("ok");
    }
}
