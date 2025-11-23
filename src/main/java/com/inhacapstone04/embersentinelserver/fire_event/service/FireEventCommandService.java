package com.inhacapstone04.embersentinelserver.fire_event.service;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.FcmService;
import com.inhacapstone04.embersentinelserver.common.service.LiveKitManagementService;
import com.inhacapstone04.embersentinelserver.common.util.LiveKitUtil;
import com.inhacapstone04.embersentinelserver.fire_event.dto.request.FireEventStartRequest;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventStreamInfoResponse;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireCause;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
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
public class FireEventCommandService {

    private final FireEventRepository fireEventRepository;
    private final CameraEdgeRepository cameraEdgeRepository;
    private final MediaStreamRepository mediaStreamRepository;

    private final LiveKitUtil liveKitUtil;
    private final FcmService fcmService;
    private final LiveKitManagementService liveKitWebhookManagementService;

    /**
     * 화재 감지 시 이벤트를 생성하고 스트리밍 환경을 구축합니다. (Publisher용)
     */
    @Transactional
    public FireEventStreamInfoResponse startFireEvent(FireEventStartRequest request) {

        // 1. CameraEdge 조회
        CameraEdge camera = cameraEdgeRepository.findByDeviceUuid(request.deviceUuid())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "해당 UUID의 카메라를 찾을 수 없습니다."));

        // 2. FireEvent 생성 및 저장
        FireEvent fireEvent = new FireEvent();
        fireEvent.setCameraEdge(camera);
        fireEvent.setFireCause(FireCause.기타);
        fireEvent.setRiskRank(0L);
        fireEventRepository.save(fireEvent);

        // 3. LiveKit Room Name 생성
        String livekitRoomName = "fire_event_" + fireEvent.getId();

        // 4. MediaStream 생성 및 저장
        MediaStream mediaStream = new MediaStream();
        mediaStream.setFireEvent(fireEvent);
        mediaStream.setLivekitRoomName(livekitRoomName);
        mediaStream.setStreamingStatus(StreamingStatus.PENDING);
        mediaStreamRepository.save(mediaStream);

        // 5. [위임] LiveKit Room 생성 및 Egress 시작 요청
        liveKitWebhookManagementService.createRoomAndStartEgress(livekitRoomName);

        // 6. Publisher Token 생성
        String metadata = "{\"type\":\"PUBLISHER\", \"cameraId\":" + camera.getId() +
                ", \"fireEventId\":" + fireEvent.getId() +
                ", \"roomId\":" + camera.getRoom().getId() + "}";

        String token = liveKitUtil.createToken(
                livekitRoomName,
                "cam_" + camera.getId(),
                camera.getCameraEdgeAlias(),
                metadata,
                true, false
        );

        // 7. FCM 알림 발송 (비동기)
        fcmService.sendFireAlert(
                camera.getRoom().getId(),
                camera.getRoom().getRoomAlias(),
                fireEvent.getId(),
                camera.getCameraEdgeAlias()
        );

        return FireEventStreamInfoResponse.of(token, livekitRoomName, fireEvent.getId());
    }
}