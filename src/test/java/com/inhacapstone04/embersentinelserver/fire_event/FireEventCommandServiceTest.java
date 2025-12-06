package com.inhacapstone04.embersentinelserver.fire_event;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.FcmService;
import com.inhacapstone04.embersentinelserver.common.service.LiveKitManagementService;
import com.inhacapstone04.embersentinelserver.common.util.LiveKitUtil;
import com.inhacapstone04.embersentinelserver.fire_event.dto.request.FireEventStartRequest;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventStreamInfoResponse;
import com.inhacapstone04.embersentinelserver.fire_event.entity.DetectionType;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventCommandService;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FireEventCommandServiceTest {

    @InjectMocks
    private FireEventCommandService fireEventCommandService;

    @Mock private FireEventRepository fireEventRepository;
    @Mock private CameraEdgeRepository cameraEdgeRepository;
    @Mock private MediaStreamRepository mediaStreamRepository;

    @Mock private LiveKitUtil liveKitUtil;
    @Mock private FcmService fcmService;
    @Mock private LiveKitManagementService liveKitManagementService;

    // 테스트 상수
    private final String DEVICE_UUID = "test-uuid-1234";
    private final Long CAMERA_ID = 10L;
    private final Long ROOM_ID = 100L;
    private final Long FIRE_EVENT_ID = 500L;
    private final String ROOM_ALIAS = "Test Room";
    private final String CAMERA_ALIAS = "Test Camera";
    private final String TOKEN = "jwt_token_example";

    // [수정] 감지 유형 상수 추가
    private final DetectionType DETECTION_TYPE = DetectionType.FIRE;

    @Test
    @DisplayName("성공: 화재 감지 요청 시 이벤트 생성, 스트리밍 시작(위임), 알림 발송, 토큰 반환이 정상적으로 수행된다.")
    void startFireEvent_Success() {
        // Given
        FireEventStartRequest request = new FireEventStartRequest(DEVICE_UUID, DETECTION_TYPE);

        // 1. Mock Camera & Room
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(ROOM_ID);
        when(room.getRoomAlias()).thenReturn(ROOM_ALIAS); // FcmService에서 사용

        CameraEdge camera = mock(CameraEdge.class);
        when(camera.getId()).thenReturn(CAMERA_ID);
        when(camera.getRoom()).thenReturn(room);
        when(camera.getCameraEdgeAlias()).thenReturn(CAMERA_ALIAS);

        when(cameraEdgeRepository.findByDeviceUuid(DEVICE_UUID)).thenReturn(Optional.of(camera));

        // 2. Mock Repository Save (ID 할당 시뮬레이션)
        when(fireEventRepository.save(any(FireEvent.class))).thenAnswer(invocation -> {
            FireEvent event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", FIRE_EVENT_ID);
            return event;
        });

        // 3. Mock Token Generation
        // [수정됨] 실제 서비스 코드에서 canSubscribe를 true로 변경했으므로, 스텁 설정도 eq(true)로 일치시켜야 함
        when(liveKitUtil.createToken(anyString(), anyString(), anyString(), anyString(), eq(true), eq(true)))
                .thenReturn(TOKEN);

        // When
        FireEventStreamInfoResponse response = fireEventCommandService.startFireEvent(request);

        // Then
        // 1. 응답 데이터 검증
        assertThat(response).isNotNull();
        assertThat(response.fireEventId()).isEqualTo(FIRE_EVENT_ID);
        assertThat(response.livekitRoomName()).isEqualTo("fire_event_" + FIRE_EVENT_ID);
        assertThat(response.token()).isEqualTo(TOKEN);

        // 2. 로직 호출 순서 및 인자 검증

        // - DB 저장 호출 확인
        verify(fireEventRepository).save(any(FireEvent.class));
        verify(mediaStreamRepository).save(any(MediaStream.class));

        // - LiveKit 인프라 제어 위임 확인
        verify(liveKitManagementService).createRoomAndStartEgress("fire_event_" + FIRE_EVENT_ID);

        // - [수정] FCM 알림 발송 서비스 호출 확인 (변경된 시그니처 반영)
        // sendFireAlert(roomId, roomAlias, fireEventId, fireDetectionType, cameraAlias)
        verify(fcmService).sendFireAlert(
                eq(ROOM_ID),
                eq(ROOM_ALIAS),
                eq(FIRE_EVENT_ID),
                eq(DETECTION_TYPE.getDescription()), // "화재" 또는 "연기"
                eq(CAMERA_ALIAS)
        );
    }

    @Test
    @DisplayName("실패: 존재하지 않는 카메라 UUID로 요청 시 NOT_FOUND 예외 발생")
    void startFireEvent_Fail_CameraNotFound() {
        // Given
        FireEventStartRequest request = new FireEventStartRequest("unknown-uuid", DETECTION_TYPE);
        when(cameraEdgeRepository.findByDeviceUuid("unknown-uuid")).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                fireEventCommandService.startFireEvent(request)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND_BY_ID);

        // 이후 로직은 실행되지 않아야 함
        verify(fireEventRepository, never()).save(any());
        verify(liveKitManagementService, never()).createRoomAndStartEgress(anyString());
    }

    @Test
    @DisplayName("실패: LiveKit 관리 서비스에서 예외 발생 시 전파 (트랜잭션 롤백 유발)")
    void startFireEvent_Fail_LiveKitError() {
        // Given
        FireEventStartRequest request = new FireEventStartRequest(DEVICE_UUID, DETECTION_TYPE);

        // [수정] 불필요한 스텁 제거 (UnnecessaryStubbingException 방지)
        // 이 테스트는 LiveKit 호출 시점에서 실패하므로, 그 이후에 호출되는 camera.getId()나 room 관련 메서드는 실행되지 않습니다.
        // 따라서 when(camera.getId())... 등의 코드는 제거해야 합니다.
        CameraEdge camera = mock(CameraEdge.class);

        when(cameraEdgeRepository.findByDeviceUuid(DEVICE_UUID)).thenReturn(Optional.of(camera));

        // DB 저장 성공 (ID 주입 - LiveKit 방 이름 생성에 필요하므로 유지)
        when(fireEventRepository.save(any(FireEvent.class))).thenAnswer(invocation -> {
            FireEvent e = invocation.getArgument(0);
            ReflectionTestUtils.setField(e, "id", FIRE_EVENT_ID);
            return e;
        });

        // LiveKit 서비스 호출 시 예외 발생 설정
        doThrow(new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR))
                .when(liveKitManagementService).createRoomAndStartEgress(anyString());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                fireEventCommandService.startFireEvent(request)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.LIVEKIT_SERVER_ERROR);

        // FCM 알림은 발송되지 않아야 함 (순서상 LiveKit 설정 후 발송되므로)
        verify(fcmService, never()).sendFireAlert(anyLong(), anyString(), anyLong(), anyString(), anyString());
    }
}
