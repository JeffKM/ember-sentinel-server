package com.inhacapstone04.embersentinelserver.livekit;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.LiveKitWebhookService;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventWebhookService;
import com.inhacapstone04.embersentinelserver.media.service.MediaRecordCommandService;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import livekit.LivekitWebhook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveKitWebhookServiceTest {

    @InjectMocks
    private LiveKitWebhookService liveKitWebhookService;

    @Mock
    private WebhookReceiver webhookReceiver;

    @Mock
    private MediaRecordCommandService mediaRecordCommandService;

    @Mock
    private FireEventWebhookService fireEventWebhookService;

    @Test
    @DisplayName("성공: participant_joined 이벤트가 오면 메타데이터를 파싱 서비스로 전달한다")
    void handleWebhookEvent_ParticipantJoined() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";
        String metadata = "{\"type\":\"PUBLISHER\"}";

        // Mock Event
        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        LivekitModels.ParticipantInfo participant = mock(LivekitModels.ParticipantInfo.class);

        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        when(event.getEvent()).thenReturn("participant_joined");

        // Participant Setup
        when(event.hasParticipant()).thenReturn(true);
        when(event.getParticipant()).thenReturn(participant);
        // getMetadata가 빈 문자열이 아님을 가정
        when(participant.getMetadata()).thenReturn(metadata);

        // When
        liveKitWebhookService.handleWebhookEvent(body, authHeader);

        // Then
        verify(fireEventWebhookService).processParticipantJoined(metadata);
        // 다른 서비스는 호출되지 않아야 함
        verify(mediaRecordCommandService, never()).saveRecording(anyString(), anyString());
    }

    @Test
    @DisplayName("성공: egress_ended 이벤트가 오고 성공 상태라면 녹화 저장 서비스를 호출한다")
    void handleWebhookEvent_EgressEnded_Complete() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";
        String roomName = "fire_event_101";
        String s3Url = "s3://bucket/file.mp4";

        // Mock Event
        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        LivekitEgress.EgressInfo egressInfo = mock(LivekitEgress.EgressInfo.class);
        LivekitEgress.FileInfo fileInfo = mock(LivekitEgress.FileInfo.class);

        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        when(event.getEvent()).thenReturn("egress_ended");

        // Egress Info Setup
        when(event.hasEgressInfo()).thenReturn(true);
        when(event.getEgressInfo()).thenReturn(egressInfo);
        when(egressInfo.getStatus()).thenReturn(LivekitEgress.EgressStatus.EGRESS_COMPLETE);
        when(egressInfo.getRoomName()).thenReturn(roomName);

        // File Info Setup
        when(egressInfo.hasFile()).thenReturn(true);
        when(egressInfo.getFile()).thenReturn(fileInfo);
        when(fileInfo.getLocation()).thenReturn(s3Url);

        // When
        liveKitWebhookService.handleWebhookEvent(body, authHeader);

        // Then
        verify(mediaRecordCommandService).saveRecording(roomName, s3Url);
        verify(fireEventWebhookService, never()).processParticipantJoined(anyString());
    }

    @Test
    @DisplayName("무시: egress_ended 이벤트지만 상태가 실패(FAILED)라면 저장하지 않는다")
    void handleWebhookEvent_EgressEnded_Failed() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";

        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        LivekitEgress.EgressInfo egressInfo = mock(LivekitEgress.EgressInfo.class);

        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        when(event.getEvent()).thenReturn("egress_ended");

        when(event.hasEgressInfo()).thenReturn(true);
        when(event.getEgressInfo()).thenReturn(egressInfo);
        when(egressInfo.getStatus()).thenReturn(LivekitEgress.EgressStatus.EGRESS_FAILED); // 실패 상태

        // When
        liveKitWebhookService.handleWebhookEvent(body, authHeader);

        // Then
        verify(mediaRecordCommandService, never()).saveRecording(anyString(), anyString());
    }

    @Test
    @DisplayName("무시: 관심 없는 이벤트 타입(room_started 등)은 무시한다")
    void handleWebhookEvent_IgnoredEvent() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";

        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        when(event.getEvent()).thenReturn("room_started"); // 처리하지 않는 이벤트

        // When
        liveKitWebhookService.handleWebhookEvent(body, authHeader);

        // Then
        verify(fireEventWebhookService, never()).processParticipantJoined(anyString());
        verify(mediaRecordCommandService, never()).saveRecording(anyString(), anyString());
    }

    @Test
    @DisplayName("실패: Webhook 서명 검증 실패 시 예외가 발생한다")
    void handleWebhookEvent_ValidationFailed() {
        // Given
        String body = "invalid-body";
        String authHeader = "invalid-token";

        // Mock Receiver to throw exception
        when(webhookReceiver.receive(body, authHeader)).thenThrow(new RuntimeException("Invalid signature"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                liveKitWebhookService.handleWebhookEvent(body, authHeader)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.LIVEKIT_SERVER_ERROR);
    }
}
