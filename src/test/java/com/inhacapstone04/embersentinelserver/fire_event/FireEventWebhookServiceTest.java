package com.inhacapstone04.embersentinelserver.fire_event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FireEventWebhookServiceTest {

    @InjectMocks
    private FireEventWebhookService fireEventWebhookService;

    @Mock
    private MediaStreamRepository mediaStreamRepository;

    // JSON 파싱 로직을 실제와 동일하게 검증하기 위해 Spy 사용
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("성공: Publisher(라즈베리파이)가 입장하면 스트리밍 상태를 LIVE로 변경한다.")
    void processParticipantJoined_Success() {
        // Given
        String metadata = "{\"type\":\"PUBLISHER\", \"fireEventId\":101}";
        Long fireEventId = 101L;

        MediaStream mediaStream = mock(MediaStream.class);
        when(mediaStream.getStreamingStatus()).thenReturn(StreamingStatus.PENDING);

        when(mediaStreamRepository.findByFireEvent_Id(fireEventId)).thenReturn(Optional.of(mediaStream));

        // When
        fireEventWebhookService.processParticipantJoined(metadata);

        // Then
        // 1. 상태 변경 메서드가 호출되었는지 검증
        verify(mediaStream).setStreamingStatus(StreamingStatus.LIVE);
    }

    @Test
    @DisplayName("무시: Subscriber(사용자)가 입장한 경우 로직을 수행하지 않는다.")
    void processParticipantJoined_Ignore_Subscriber() {
        // Given
        String metadata = "{\"type\":\"SUBSCRIBER\", \"userId\":10}";

        // When
        fireEventWebhookService.processParticipantJoined(metadata);

        // Then
        // Repository 조회조차 일어나지 않아야 함
        verify(mediaStreamRepository, never()).findByFireEvent_Id(anyLong());
    }

    @Test
    @DisplayName("무시: 메타데이터가 비어있거나 null인 경우")
    void processParticipantJoined_Ignore_EmptyMetadata() {
        // Given
        String emptyMetadata = "";
        String nullMetadata = null;

        // When
        fireEventWebhookService.processParticipantJoined(emptyMetadata);
        fireEventWebhookService.processParticipantJoined(nullMetadata);

        // Then
        verify(mediaStreamRepository, never()).findByFireEvent_Id(anyLong());
    }

    @Test
    @DisplayName("무시: Publisher지만 fireEventId가 없는 경우 (잘못된 메타데이터)")
    void processParticipantJoined_Ignore_MissingFireEventId() {
        // Given
        String metadata = "{\"type\":\"PUBLISHER\", \"cameraId\":5}"; // fireEventId 누락

        // When
        fireEventWebhookService.processParticipantJoined(metadata);

        // Then
        verify(mediaStreamRepository, never()).findByFireEvent_Id(anyLong());
    }

    @Test
    @DisplayName("실패: 해당하는 MediaStream을 찾을 수 없는 경우 (예외 발생)")
    void processParticipantJoined_Fail_MediaStreamNotFound() {
        // Given
        String metadata = "{\"type\":\"PUBLISHER\", \"fireEventId\":999}";
        Long fireEventId = 999L;

        when(mediaStreamRepository.findByFireEvent_Id(fireEventId)).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                fireEventWebhookService.processParticipantJoined(metadata)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND_BY_ID);
    }

    @Test
    @DisplayName("예외 처리: 잘못된 JSON 형식인 경우 (로그만 남기고 예외 던지지 않음)")
    void processParticipantJoined_JsonError() {
        // Given
        String brokenJson = "{type:PUBLISHER, fireEventId:101"; // 따옴표 누락 등 문법 오류

        // When & Then
        // 서비스 내부에서 try-catch로 처리하므로 예외가 밖으로 던져지면 안 됨
        assertDoesNotThrow(() -> fireEventWebhookService.processParticipantJoined(brokenJson));

        // Repository 호출이 없어야 함
        verify(mediaStreamRepository, never()).findByFireEvent_Id(anyLong());
    }

    @Test
    @DisplayName("로직 수행: 이미 LIVE 상태인 경우 상태 변경을 중복으로 호출하지 않는다.")
    void processParticipantJoined_AlreadyLive() {
        // Given
        String metadata = "{\"type\":\"PUBLISHER\", \"fireEventId\":101}";
        Long fireEventId = 101L;

        MediaStream mediaStream = mock(MediaStream.class);
        // 이미 LIVE 상태
        when(mediaStream.getStreamingStatus()).thenReturn(StreamingStatus.LIVE);

        when(mediaStreamRepository.findByFireEvent_Id(fireEventId)).thenReturn(Optional.of(mediaStream));

        // When
        fireEventWebhookService.processParticipantJoined(metadata);

        // Then
        // 상태 변경(setter)이 호출되지 않아야 함
        verify(mediaStream, never()).setStreamingStatus(any());
    }
}
