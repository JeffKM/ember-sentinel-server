package com.inhacapstone04.embersentinelserver.fire_event.service;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.FcmService;
import com.inhacapstone04.embersentinelserver.common.util.LiveKitUtil;
import com.inhacapstone04.embersentinelserver.fire_event.dto.request.FireEventStartRequest;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventStreamInfoResponse;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireCause;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FireEventCommandService {

    private final FireEventRepository fireEventRepository;
    private final CameraEdgeRepository cameraEdgeRepository;
    private final MediaStreamRepository mediaStreamRepository;

    private final LiveKitUtil liveKitUtil;
    private final RoomServiceClient roomServiceClient;
    private final EgressServiceClient egressServiceClient;
    private final FcmService fcmService;

    /**
     * 화재 감지 시 이벤트를 생성하고 스트리밍 환경을 구축합니다. (Publisher용)
     */
    @Transactional
    public FireEventStreamInfoResponse startFireEvent(FireEventStartRequest request) {

        // 1. CameraEdge 조회
        CameraEdge camera = cameraEdgeRepository.findByDeviceUuid(request.deviceUuid())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "해당 UUID의 카메라를 찾을 수 없습니다."));

        // 2. FireEvent 생성
        FireEvent fireEvent = new FireEvent();
        fireEvent.setCameraEdge(camera);
        fireEvent.setFireCause(FireCause.기타);
        fireEvent.setRiskRank(0L);
        fireEventRepository.save(fireEvent);

        // 3. LiveKit Room Name 생성
        String livekitRoomName = "fire_event_" + fireEvent.getId();

        // 4. MediaStream 생성
        MediaStream mediaStream = new MediaStream();
        mediaStream.setFireEvent(fireEvent);
        mediaStream.setLivekitRoomName(livekitRoomName);
        mediaStream.setStreamingStatus(StreamingStatus.PENDING);
        mediaStreamRepository.save(mediaStream);

        // 5. [LiveKit API] Room 생성 및 녹화 시작
        try {
            // 5-1. Room 생성 요청
            Call<LivekitModels.Room> roomCall = roomServiceClient.createRoom(livekitRoomName);
            Response<LivekitModels.Room> roomResponse = roomCall.execute();

            if (!roomResponse.isSuccessful()) {
                String errorMsg = roomResponse.errorBody() != null ? roomResponse.errorBody().string() : "Unknown error";
                log.error("LiveKit Room creation failed: {}", errorMsg);
                // 실패 시 예외를 던져 트랜잭션 롤백
                throw new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR, "Room 생성 실패: " + errorMsg);
            }
            log.info("LiveKit Room created: {}", livekitRoomName);

            // 5-2. Egress(녹화) 시작 요청
            LivekitEgress.EncodedFileOutput output = LivekitEgress.EncodedFileOutput.newBuilder()
                    .setFileType(LivekitEgress.EncodedFileType.MP4)
                    .setFilepath("recordings/" + livekitRoomName + ".mp4")
                    .build();

            Call<LivekitEgress.EgressInfo> egressCall = egressServiceClient.startRoomCompositeEgress(
                    livekitRoomName,    // roomName
                    output,             // output
                    "single-speaker"    // layout
            );

            Response<LivekitEgress.EgressInfo> egressResponse = egressCall.execute();

            if (egressResponse.isSuccessful()) {
                log.info("Egress started for room: {}, EgressID: {}", livekitRoomName, egressResponse.body().getEgressId());
            } else {
                String errorMsg = egressResponse.errorBody() != null ? egressResponse.errorBody().string() : "Unknown error";
                log.error("Failed to start egress: {}", errorMsg);
                // 녹화 시작 실패도 중요한 오류라면 롤백 (선택 사항이지만 데이터 정합성을 위해 롤백 추천)
                throw new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR, "Egress 시작 실패: " + errorMsg);
            }

        } catch (IOException e) {
            log.error("Network error while communicating with LiveKit server", e);
            // IO 예외 발생 시 CustomException으로 감싸서 던짐 -> 트랜잭션 롤백
            throw new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR, "LiveKit 네트워크 오류: " + e.getMessage());
        }

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

        // 7. FCM 알림 발송 (비동기 호출), 트랜잭션이 커밋된 후 발송되거나, 별도 스레드에서 실행됨
        fcmService.sendFireAlert(
                camera.getRoom().getId(),
                camera.getRoom().getRoomAlias(),
                fireEvent.getId(),
                camera.getCameraEdgeAlias()
        );

        return FireEventStreamInfoResponse.of(token, livekitRoomName, fireEvent.getId());
    }
}