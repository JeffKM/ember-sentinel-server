package com.inhacapstone04.embersentinelserver.fire_event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FireEventWebhookService {

    private final MediaStreamRepository mediaStreamRepository;
    private final ObjectMapper objectMapper; // JSON 파싱을 위해 주입

    /**
     * 참여자 입장(participant_joined) Webhook 이벤트를 처리합니다.
     * 메타데이터를 확인하여 Publisher(라즈베리파이)인 경우 스트리밍 상태를 LIVE로 변경합니다.
     *
     * @param metadataJson LiveKit 참여자 메타데이터 (JSON String)
     */
    @Transactional
    public void processParticipantJoined(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty()) {
            log.debug("Metadata is empty, skipping logic.");
            return;
        }

        try {
            // 1. JSON 파싱
            JsonNode metadataNode = objectMapper.readTree(metadataJson);

            // 2. 참여자 타입 확인 ("type": "PUBLISHER" 인지 검증)
            if (metadataNode.has("type") && "PUBLISHER".equals(metadataNode.get("type").asText())) {

                // 3. FireEvent ID 추출
                if (!metadataNode.has("fireEventId")) {
                    log.warn("Publisher metadata missing fireEventId: {}", metadataJson);
                    return;
                }
                Long fireEventId = metadataNode.get("fireEventId").asLong();

                // 4. MediaStream 조회 및 상태 업데이트 (PENDING -> LIVE)
                MediaStream mediaStream = mediaStreamRepository.findByFireEvent_Id(fireEventId)
                        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "MediaStream not found for FireEvent ID: " + fireEventId));

                // 이미 LIVE거나 ENDED인 경우 불필요한 업데이트 방지
                if (mediaStream.getStreamingStatus() == StreamingStatus.PENDING) {
                    mediaStream.setStreamingStatus(StreamingStatus.LIVE);
                    log.info("Streaming Status Updated to LIVE for FireEvent ID: {}", fireEventId);
                }
            } else {
                log.debug("Participant is not a PUBLISHER. Ignoring.");
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse participant metadata: {}", metadataJson, e);
            // Webhook 처리는 실패하더라도 200 OK를 반환해야 LiveKit이 재전송하지 않으므로 예외를 로그만 남김
        }
    }
}
