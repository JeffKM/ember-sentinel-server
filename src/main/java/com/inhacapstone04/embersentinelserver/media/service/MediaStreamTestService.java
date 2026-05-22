package com.inhacapstone04.embersentinelserver.media.service;

import com.inhacapstone04.embersentinelserver.common.service.LiveKitManagementService;
import com.inhacapstone04.embersentinelserver.common.util.LiveKitUtil;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventStreamInfoResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventWatchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "livekit.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MediaStreamTestService {

    private final LiveKitManagementService liveKitManagementService;
    private final LiveKitUtil liveKitUtil;

    /**
     * LiveKit 인프라 테스트용 메서드 (DB 저장 X)
     * 임의의 방을 생성하고 녹화를 시작한 뒤, 접속 가능한 토큰을 반환합니다.
     */
    public FireEventStreamInfoResponse testStartStreaming() {
        // 1. 테스트용 ID 생성 (DB에 저장하지 않으므로 임의의 값 사용)
        Long testFireEventId = System.currentTimeMillis(); // 임시 ID
        String livekitRoomName = "test_stream_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Starting LiveKit Test Stream: {}", livekitRoomName);

        // 2. LiveKit Room 생성 및 Egress 시작 요청 (인프라 테스트)
        liveKitManagementService.createRoomAndStartEgress(livekitRoomName);

        // 3. Publisher Token 생성
        // 테스트용 메타데이터 주입
        String metadata = "{\"type\":\"TEST_PUBLISHER\", \"fireEventId\":" + testFireEventId + "}";

        String token = liveKitUtil.createToken(
                livekitRoomName,
                "test_cam_" + testFireEventId, // Identity
                "Test Camera",                 // Name
                metadata,
                true,  // canPublish: TRUE
                true   // canSubscribe: TRUE (테스트 편의를 위해 둘 다 허용)
        );

        // 4. 결과 반환
        return FireEventStreamInfoResponse.of(token, livekitRoomName, testFireEventId);
    }

    /**
     * 테스트용 시청자(Subscriber) 토큰 발급 API
     * 특정 roomName에 대해 시청 권한만 있는 토큰을 발급합니다.
     *
     * @param roomName 접속할 방 이름
     * @return FireEventWatchResponse (토큰 정보)
     */
    public FireEventWatchResponse testSubscribe(String roomName) {
        Long tempViewerId = System.currentTimeMillis();
        String identity = "test_viewer_" + tempViewerId;

        // 테스트용 메타데이터 주입
        String metadata = "{\"type\":\"TEST_SUBSCRIBER\", \"userId\":" + tempViewerId + "}";

        String token = liveKitUtil.createToken(
                roomName,
                identity,           // Identity
                "Test Viewer",      // Name
                metadata,
                false, // canPublish: FALSE (시청자는 송출 불가)
                true   // canSubscribe: TRUE (시청 가능)
        );

        // DB가 없으므로 fireEventId는 임시값(현재시간 등)을 반환
        return FireEventWatchResponse.of(token, roomName, tempViewerId);
    }

    /**
     * 테스트용 스트리밍 종료 API
     * 방을 삭제하면 Egress도 자동으로 종료되고 S3 업로드가 시작됩니다.
     *
     * @param roomName 삭제할 LiveKit 방 이름
     */
    public void testStopStreaming(String roomName) {
        log.info("Stopping LiveKit Test Stream: {}", roomName);
        liveKitManagementService.deleteRoom(roomName);
    }
}
