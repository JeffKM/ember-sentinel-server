package com.inhacapstone04.embersentinelserver.common.service;

import com.google.firebase.messaging.*;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final UserRoomMembershipRepository membershipRepository;
    private final FirebaseMessaging firebaseMessaging;

    /**
     * 화재 발생 알림을 해당 방의 모든 멤버에게 전송합니다. (비동기 처리)
     *
     * @param roomId 화재가 발생한 방 ID
     * @param fireEventId 생성된 화재 이벤트 ID
     * @param cameraAlias 감지한 카메라 이름
     */
    @Async
    public void sendFireAlert(Long roomId, String roomAlias, Long fireEventId, String fireDetectionType, String cameraAlias) {
        if (firebaseMessaging == null) {
            log.info("[로컬] FCM 비활성화 — 화재 알림 전송 생략 (Room: {}, Event: {})", roomId, fireEventId);
            return;
        }
        try {
            // 1. 해당 방 멤버들의 FCM 토큰 조회
            List<String> tokens = membershipRepository.findAllFcmTokensByRoomId(roomId);

            List<String> validTokens = tokens.stream()
                    .filter(token -> token != null && !token.trim().isEmpty())
                    .toList();

            if (validTokens.isEmpty()) {
                log.info("No valid FCM tokens found for Room ID: {}", roomId);
                return;
            }

            // 2. 알림 메시지 구성
            // roomAlias가 제목에 포함되는지 확인 필요
            Notification notification = Notification.builder()
                    .setTitle("[Ember Sentinel]🔥 " + roomAlias + "에서 " + fireDetectionType + " 감지 알림")
                    .setBody(String.format("[%s] 카메라에서 화재가 감지되었습니다! 탭하여 확인하세요.", cameraAlias))
                    .build();

            // 3. 데이터 메시지 구성
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(notification)
                    .putData("type", "FIRE_ALERT")
                    .putData("roomId", String.valueOf(roomId))
                    .putData("fireEventId", String.valueOf(fireEventId))
                    .addAllTokens(validTokens)
                    .build();

            // 4. 발송
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM sent successfully. Success: {}, Failure: {}", response.getSuccessCount(), response.getFailureCount());

        } catch (Exception e) {
            log.error("Failed to send FCM alert", e);
        }
    }

    /**
     * 특정 사용자(토큰)에게 간단한 알림 메시지를 전송합니다. (비동기 처리)
     *
     * @param token 대상 사용자의 FCM 토큰
     * @param alertBody 알림 내용
     */
    @Async
    public void sendSimpleAlertByFcm(String token, String alertBody) {
        if (firebaseMessaging == null) {
            log.info("[로컬] FCM 비활성화 — 알림 전송 생략 (token: {})", token);
            return;
        }
        try {
            // 1. 알림 구성 (제목은 기본값으로 설정, 필요시 파라미터로 분리 가능)
            Notification notification = Notification.builder()
                    .setTitle("[Ember Sentinel] 알림")
                    .setBody(alertBody)
                    .build();

            // 2. 단일 메시지 구성
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    // 필요하다면 추가 데이터 payload를 넣을 수 있습니다.
                    // .putData("type", "SIMPLE_ALERT")
                    .build();

            // 3. 발송
            String response = firebaseMessaging.send(message);
            log.info("Simple FCM sent successfully. Response: {}", response);

        } catch (FirebaseMessagingException e) {
            // FCM 전송 실패 시 에러 코드 등에 따라 처리가 필요할 수 있음 (예: 토큰 만료 등)
            log.error("Failed to send simple FCM to token: {}", token, e);
        } catch (Exception e) {
            log.error("Unexpected error during FCM sending", e);
        }
    }
}
