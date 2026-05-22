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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class FireEventCommandService {

    private final FireEventRepository fireEventRepository;
    private final CameraEdgeRepository cameraEdgeRepository;
    private final MediaStreamRepository mediaStreamRepository;
    private final FcmService fcmService;

    // LiveKit 비활성화 시 빈이 등록되지 않으므로 Optional로 주입
    private final Optional<LiveKitUtil> liveKitUtil;
    private final Optional<LiveKitManagementService> liveKitManagementService;

    public FireEventCommandService(
            FireEventRepository fireEventRepository,
            CameraEdgeRepository cameraEdgeRepository,
            MediaStreamRepository mediaStreamRepository,
            FcmService fcmService,
            Optional<LiveKitUtil> liveKitUtil,
            Optional<LiveKitManagementService> liveKitManagementService
    ) {
        this.fireEventRepository = fireEventRepository;
        this.cameraEdgeRepository = cameraEdgeRepository;
        this.mediaStreamRepository = mediaStreamRepository;
        this.fcmService = fcmService;
        this.liveKitUtil = liveKitUtil;
        this.liveKitManagementService = liveKitManagementService;
    }

    /**
     * 화재 감지 시 이벤트를 생성하고 스트리밍 환경을 구축합니다. (Publisher용)
     *
     * @param request 화재 이벤트 시작 요청
     * @param deviceCameraEdgeId DeviceAuthInterceptor에서 인증된 카메라 ID
     */
    @Transactional
    public FireEventStreamInfoResponse startFireEvent(FireEventStartRequest request, Long deviceCameraEdgeId) {

        // 1. CameraEdge 조회
        CameraEdge camera = cameraEdgeRepository.findByDeviceUuid(request.deviceUuid())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "해당 UUID의 카메라를 찾을 수 없습니다."));

        // 1-1. API Key로 인증된 디바이스와 요청 body의 deviceUuid가 일치하는지 검증
        if (!camera.getId().equals(deviceCameraEdgeId)) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_API_KEY, "API Key와 deviceUuid가 일치하지 않습니다.");
        }

        // 2. FireEvent 생성 및 저장
        FireEvent fireEvent = new FireEvent();
        fireEvent.setCameraEdge(camera);
        fireEvent.setDetectionType(request.detectionType());
        fireEvent.setFireCause(FireCause.기타); // 현재 모델에 포함되지 않은 기능, 추후 개발을 위해 미리 개발
        fireEvent.setRiskRank(0L);             // 현재 모델에 포함되지 않은 기능, 추후 개발을 위해 미리 개발
        fireEventRepository.save(fireEvent);

        // 3. LiveKit Room Name 생성
        String livekitRoomName = "fire_event_" + fireEvent.getId();

        // 4. MediaStream 생성 및 저장
        MediaStream mediaStream = new MediaStream();
        mediaStream.setFireEvent(fireEvent);
        mediaStream.setLivekitRoomName(livekitRoomName);
        mediaStream.setStreamingStatus(StreamingStatus.PENDING);
        mediaStreamRepository.save(mediaStream);

        // 5. [위임] LiveKit Room 생성 및 Egress 시작 요청 (LiveKit 비활성화 시 스킵)
        liveKitManagementService.ifPresentOrElse(
                service -> service.createRoomAndStartEgress(livekitRoomName),
                () -> log.warn("LiveKit 비활성화 상태 — Room 생성 및 Egress 시작을 스킵합니다.")
        );

        // 6. Publisher Token 생성 (LiveKit 비활성화 시 빈 토큰 반환)
        String metadata = "{\"type\":\"PUBLISHER\", \"cameraId\":" + camera.getId() +
                ", \"fireEventId\":" + fireEvent.getId() +
                ", \"roomId\":" + camera.getRoom().getId() + "}";

        String token = liveKitUtil.map(util -> util.createToken(
                livekitRoomName,
                "cam_" + camera.getId(),
                camera.getCameraEdgeAlias(),
                metadata,
                true, true
        )).orElse("");

        // 7. FCM 알림 발송 (비동기)
        fcmService.sendFireAlert(
                camera.getRoom().getId(),
                camera.getRoom().getRoomAlias(),
                fireEvent.getId(),
                fireEvent.getDetectionType().getDescription(),
                camera.getCameraEdgeAlias()
        );

        return FireEventStreamInfoResponse.of(token, livekitRoomName, fireEvent.getId());
    }
}