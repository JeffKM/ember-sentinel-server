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
public class LiveKitWebhookEventSeperationService {

    private final WebhookReceiver webhookReceiver;
    private final MediaRecordCommandService mediaRecordCommandService;
    private final FireEventWebhookService fireEventWebhookService;

    public void handleWebhookEvent(String body, String authHeader) {
        try {
            // 1. 서명 검증
            LivekitWebhook.WebhookEvent event = webhookReceiver.receive(body, authHeader);
            String eventType = event.getEvent();
            log.info("LiveKit Webhook Verified: {}", eventType);

            // 2. 이벤트 타입별 로직 분기
            switch (eventType) {
                case "participant_joined":
                    handleParticipantJoined(event);
                    break;
                case "participant_left":
                case "participant_disconnected":
                    handleParticipantDisconnected(event);
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
            // [수정됨] DB 상태 변경 로직이 있는 FireEventWebhookService 호출
            if (metadata != null && !metadata.isEmpty()) {
                fireEventWebhookService.processParticipantJoined(metadata);
            }
        }
    }

    private void handleParticipantDisconnected(LivekitWebhook.WebhookEvent event) {
        if (event.hasParticipant()) {
            String metadata = event.getParticipant().getMetadata();
            String roomName = event.getRoom().getName();

            // [수정됨] DB 상태 변경 및 종료 처리를 위해 FireEventWebhookService 호출
            if (metadata != null && !metadata.isEmpty()) {
                fireEventWebhookService.processParticipantDisconnected(metadata, roomName);
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