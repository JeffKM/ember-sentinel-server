package com.inhacapstone04.embersentinelserver.common.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventWebhookService;
import com.inhacapstone04.embersentinelserver.media.service.MediaRecordCommandService;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitEgress;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveKitWebhookService {

    private final WebhookReceiver webhookReceiver;
    private final MediaRecordCommandService mediaRecordCommandService;
    private final FireEventWebhookService fireEventWebhookService;

    /**
     * LiveKit Webhook 이벤트를 수신하여 검증하고, 적절한 도메인 서비스로 분배합니다.
     *
     * @param body       Webhook 요청 본문
     * @param authHeader Authorization 헤더 (서명)
     */
    public void handleWebhookEvent(String body, String authHeader) {
        try {
            // 1. 서명 검증
            LivekitWebhook.WebhookEvent event = webhookReceiver.receive(body, authHeader);
            String eventType = event.getEvent();
            log.info("LiveKit Webhook Verified: {}", eventType);

            // 2. 이벤트 타입별 로직 분기 (Routing)
            switch (eventType) {
                case "participant_joined":
                    handleParticipantJoined(event);
                    break;
                case "egress_ended":
                    handleEgressEnded(event);
                    break;
                default:
                    log.debug("Ignored LiveKit Event: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("Webhook handling failed", e);
            throw new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR, "Webhook validation or processing failed");
        }
    }

    private void handleParticipantJoined(LivekitWebhook.WebhookEvent event) {
        if (event.hasParticipant()) {
            String metadata = event.getParticipant().getMetadata();

            if (metadata != null && !metadata.isEmpty()) {
                fireEventWebhookService.processParticipantJoined(metadata);
            }
        }
    }

    private void handleEgressEnded(LivekitWebhook.WebhookEvent event) {
        if (event.hasEgressInfo()) {
            LivekitEgress.EgressInfo egressInfo = event.getEgressInfo();

            if (egressInfo.getStatus() == LivekitEgress.EgressStatus.EGRESS_COMPLETE) {
                String roomName = egressInfo.getRoomName();

                // File Output의 경우 location에 S3 URL이 담김
                if (egressInfo.hasFile()) {
                    String s3Url = egressInfo.getFile().getLocation();
                    log.info("Egress Ended Successfully: Room={}, URL={}", roomName, s3Url);
                    mediaRecordCommandService.saveRecording(roomName, s3Url);
                }
            } else {
                log.warn("Egress Failed or Stopped: Status={}", egressInfo.getStatus());
            }
        }
    }
}
