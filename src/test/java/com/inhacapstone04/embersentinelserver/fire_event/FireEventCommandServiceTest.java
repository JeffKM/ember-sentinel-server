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

    @Test
    @DisplayName("성공: 화재 감지 요청 시 이벤트 생성, 스트리밍 시작, 알림 발송, 토큰 반환이 정상적으로 수행된다.")
    void startFireEvent_Success() {
        // Given
        FireEventStartRequest request = new FireEventStartRequest(DEVICE_UUID);

        // 1. Mock Camera & Room
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(ROOM_ID);
        when(room.getRoomAlias()).thenReturn(ROOM_ALIAS);

        CameraEdge camera = mock(CameraEdge.class);
        when(camera.getId()).thenReturn(CAMERA_ID);
        when(camera.getRoom()).thenReturn(room);
        when(camera.getCameraEdgeAlias()).thenReturn(CAMERA_ALIAS);

        when(cameraEdgeRepository.findByDeviceUuid(DEVICE_UUID)).thenReturn(Optional.of(camera));

        // 2. Mock Repository Save (ID 할당 시뮬레이션)
        // 서비스 내부에서 new FireEvent()를 호출하므로, save 메서드 호출 시 ID를 강제로 주입해줍니다.
        when(fireEventRepository.save(any(FireEvent.class))).thenAnswer(invocation -> {
            FireEvent event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", FIRE_EVENT_ID);
            return event;
        });

        // 3. Mock Token Generation
        when(liveKitUtil.createToken(anyString(), anyString(), anyString(), anyString(), eq(true), eq(false)))
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

        // - LiveKit 인프라 제어 서비스 호출 확인 (방 생성 & 녹화 시작)
        verify(liveKitManagementService).createRoomAndStartEgress("fire_event_" + FIRE_EVENT_ID);

        // - FCM 알림 발송 서비스 호출 확인
        verify(fcmService).sendFireAlert(eq(ROOM_ID), eq(ROOM_ALIAS), eq(FIRE_EVENT_ID), eq(CAMERA_ALIAS));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 카메라 UUID로 요청 시 NOT_FOUND 예외 발생")
    void startFireEvent_Fail_CameraNotFound() {
        // Given
        FireEventStartRequest request = new FireEventStartRequest("unknown-uuid");
        when(cameraEdgeRepository.findByDeviceUuid("unknown-uuid")).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                fireEventCommandService.startFireEvent(request)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND_BY_ID);

        // 이후 로직은 실행되지 않아야 함
        verify(fireEventRepository, never()).save(any());
        verify(liveKitManagementService, never()).createRoomAndStartEgress(anyString());
        verify(fcmService, never()).sendFireAlert(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("실패: LiveKit 관리 서비스에서 예외 발생 시 전파 (트랜잭션 롤백 유발)")
    void startFireEvent_Fail_LiveKitError() {
        // Given
        FireEventStartRequest request = new FireEventStartRequest(DEVICE_UUID);

        // 카메라 조회 성공
        CameraEdge camera = mock(CameraEdge.class);
        when(cameraEdgeRepository.findByDeviceUuid(DEVICE_UUID)).thenReturn(Optional.of(camera));

        // DB 저장 성공 (ID 주입)
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
        verify(fcmService, never()).sendFireAlert(anyLong(), anyString(), anyLong(), anyString());
    }
}
