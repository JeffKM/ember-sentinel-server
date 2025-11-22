package com.inhacapstone04.embersentinelserver.common.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
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
    // 단위 테스트의 효율성을 FirebaseMessaging.getInstance()를 의존성 주입으로 리팩토링
    private final FirebaseMessaging firebaseMessaging;

    /**
     * 화재 발생 알림을 해당 방의 모든 멤버에게 전송합니다. (비동기 처리)
     *
     * @param roomId 화재가 발생한 방 ID
     * @param fireEventId 생성된 화재 이벤트 ID
     * @param cameraAlias 감지한 카메라 이름
     */
    @Async
    public void sendFireAlert(Long roomId, String roomAlias, Long fireEventId, String cameraAlias) {
        try {
            // 1. 해당 방 멤버들의 FCM 토큰 조회
            List<String> tokens = membershipRepository.findAllFcmTokensByRoomId(roomId);

            if (tokens.isEmpty()) {
                log.info("No FCM tokens found for Room ID: {}", roomId);
                return;
            }

            // 2. 알림 메시지 구성
            // roomAlias가 제목에 포함되는지 확인 필요
            Notification notification = Notification.builder()
                    .setTitle("[Ember Sentinel]🔥 " + roomAlias + "에서 화재 감지 알림")
                    .setBody(String.format("[%s] 카메라에서 화재가 감지되었습니다! 탭하여 확인하세요.", cameraAlias))
                    .build();

            // 3. 데이터 메시지 구성
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(notification)
                    .putData("type", "FIRE_ALERT")
                    .putData("roomId", String.valueOf(roomId))
                    .putData("fireEventId", String.valueOf(fireEventId))
                    .addAllTokens(tokens)
                    .build();

            // 4. 발송
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM sent successfully. Success: {}, Failure: {}", response.getSuccessCount(), response.getFailureCount());

        } catch (Exception e) {
            log.error("Failed to send FCM alert", e);
        }
    }
}
