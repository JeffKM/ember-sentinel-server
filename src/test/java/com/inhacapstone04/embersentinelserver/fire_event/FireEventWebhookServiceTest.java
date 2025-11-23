package com.inhacapstone04.embersentinelserver.fire_event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.LiveKitManagementService;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventWebhookService;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FireEventWebhookServiceTest {

    @InjectMocks
    private FireEventWebhookService fireEventWebhookService;

    @Mock
    private MediaStreamRepository mediaStreamRepository;

    @Mock
    private LiveKitManagementService liveKitManagementService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private final Long FIRE_EVENT_ID = 101L;
    private final String PUBLISHER_METADATA = "{\"type\":\"PUBLISHER\", \"fireEventId\":101}";
    private final String SUBSCRIBER_METADATA = "{\"type\":\"SUBSCRIBER\", \"userId\":10}";
    private final String ROOM_NAME = "fire_event_101";

    // --- processParticipantJoined Tests ---

    @Test
    @DisplayName("입장 성공: Publisher가 입장하면 스트리밍 상태를 LIVE로 변경한다.")
    void processParticipantJoined_Success() {
        // Given
        MediaStream mediaStream = mock(MediaStream.class);
        when(mediaStream.getStreamingStatus()).thenReturn(StreamingStatus.PENDING);
        when(mediaStreamRepository.findByFireEvent_Id(FIRE_EVENT_ID)).thenReturn(Optional.of(mediaStream));

        // When
        fireEventWebhookService.processParticipantJoined(PUBLISHER_METADATA);

        // Then
        verify(mediaStream).setStreamingStatus(StreamingStatus.LIVE);
    }

    @Test
    @DisplayName("입장 무시: Subscriber가 입장한 경우 로직을 수행하지 않는다.")
    void processParticipantJoined_Ignore_Subscriber() {
        // When
        fireEventWebhookService.processParticipantJoined(SUBSCRIBER_METADATA);

        // Then
        verify(mediaStreamRepository, never()).findByFireEvent_Id(anyLong());
    }

    @Test
    @DisplayName("입장 실패: MediaStream을 찾을 수 없는 경우 예외 발생")
    void processParticipantJoined_Fail_NotFound() {
        // Given
        when(mediaStreamRepository.findByFireEvent_Id(FIRE_EVENT_ID)).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                fireEventWebhookService.processParticipantJoined(PUBLISHER_METADATA)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND_BY_ID);
    }

    // --- processParticipantDisconnected Tests ---

    @Test
    @DisplayName("퇴장 성공: Publisher가 연결을 끊으면 상태를 ENDED로 변경하고 방 삭제를 요청한다.")
    void processParticipantDisconnected_Success() {
        // Given
        MediaStream mediaStream = mock(MediaStream.class);
        // 현재 상태 LIVE 가정
        when(mediaStream.getStreamingStatus()).thenReturn(StreamingStatus.LIVE);
        when(mediaStreamRepository.findByFireEvent_Id(FIRE_EVENT_ID)).thenReturn(Optional.of(mediaStream));

        // When
        fireEventWebhookService.processParticipantDisconnected(PUBLISHER_METADATA, ROOM_NAME);

        // Then
        // 1. DB 상태 변경 확인
        verify(mediaStream).setStreamingStatus(StreamingStatus.ENDED);

        // 2. LiveKitManagementService.deleteRoom 호출 확인 (핵심 변경점)
        verify(liveKitManagementService).deleteRoom(ROOM_NAME);
    }

    @Test
    @DisplayName("퇴장 무시: Subscriber가 나간 경우 아무 작업도 하지 않는다.")
    void processParticipantDisconnected_Ignore_Subscriber() {
        // When
        fireEventWebhookService.processParticipantDisconnected(SUBSCRIBER_METADATA, ROOM_NAME);

        // Then
        verify(mediaStreamRepository, never()).findByFireEvent_Id(anyLong());
        verify(liveKitManagementService, never()).deleteRoom(anyString());
    }

    @Test
    @DisplayName("퇴장 예외 처리: DB 업데이트 중 예외가 발생해도 로그만 남기고 종료한다 (WebHook 200 OK 보장을 위해)")
    void processParticipantDisconnected_Exception_Handled() {
        // Given
        // DB 조회 실패 시 예외 발생
        when(mediaStreamRepository.findByFireEvent_Id(FIRE_EVENT_ID)).thenReturn(Optional.empty());

        // When & Then
        // 서비스 내부의 catch(Exception e) 블록에 의해 예외가 던져지지 않아야 함
        assertDoesNotThrow(() ->
                fireEventWebhookService.processParticipantDisconnected(PUBLISHER_METADATA, ROOM_NAME)
        );

        // 예외가 발생했으므로 방 삭제 로직까지 도달하지 않아야 함
        verify(liveKitManagementService, never()).deleteRoom(anyString());
    }

    @Test
    @DisplayName("퇴장 로직: 이미 ENDED 상태라면 상태 변경을 중복 호출하지 않지만, 방 삭제는 시도한다.")
    void processParticipantDisconnected_AlreadyEnded() {
        // Given
        MediaStream mediaStream = mock(MediaStream.class);
        when(mediaStream.getStreamingStatus()).thenReturn(StreamingStatus.ENDED);
        when(mediaStreamRepository.findByFireEvent_Id(FIRE_EVENT_ID)).thenReturn(Optional.of(mediaStream));

        // When
        fireEventWebhookService.processParticipantDisconnected(PUBLISHER_METADATA, ROOM_NAME);

        // Then
        // 상태 변경 setter는 호출되지 않음
        verify(mediaStream, never()).setStreamingStatus(any());

        // 하지만 방 삭제 요청은 안전을 위해 호출되어야 함
        verify(liveKitManagementService).deleteRoom(ROOM_NAME);
    }
}
