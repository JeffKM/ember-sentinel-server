package com.inhacapstone04.embersentinelserver.livekit;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.LiveKitWebhookEventSeperationService;
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
class LiveKitWebhookEventSeperationServiceTest {

    @InjectMocks
    private LiveKitWebhookEventSeperationService seperationService;

    @Mock
    private WebhookReceiver webhookReceiver;

    @Mock
    private MediaRecordCommandService mediaRecordCommandService;

    @Mock
    private FireEventWebhookService fireEventWebhookService;

    // --- Participant Joined Tests ---

    @Test
    @DisplayName("라우팅 성공: participant_joined 이벤트는 FireEventWebhookService로 위임한다")
    void handleWebhookEvent_ParticipantJoined() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";
        String metadata = "{\"type\":\"PUBLISHER\"}";

        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        LivekitModels.ParticipantInfo participant = mock(LivekitModels.ParticipantInfo.class);

        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        when(event.getEvent()).thenReturn("participant_joined");

        when(event.hasParticipant()).thenReturn(true);
        when(event.getParticipant()).thenReturn(participant);
        when(participant.getMetadata()).thenReturn(metadata);

        // When
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        verify(fireEventWebhookService).processParticipantJoined(metadata);
        verify(mediaRecordCommandService, never()).saveRecording(anyString(), anyString());
    }

    // --- Participant Disconnected Tests ---

    @Test
    @DisplayName("라우팅 성공: participant_disconnected 이벤트는 FireEventWebhookService로 위임한다")
    void handleWebhookEvent_ParticipantDisconnected() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";
        String metadata = "{\"type\":\"PUBLISHER\"}";
        String roomName = "fire_event_101";

        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        LivekitModels.ParticipantInfo participant = mock(LivekitModels.ParticipantInfo.class);
        LivekitModels.Room room = mock(LivekitModels.Room.class);

        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        // disconnected 또는 left 둘 다 처리되는지 확인
        when(event.getEvent()).thenReturn("participant_disconnected");

        when(event.hasParticipant()).thenReturn(true);
        when(event.getParticipant()).thenReturn(participant);
        when(participant.getMetadata()).thenReturn(metadata);

        when(event.getRoom()).thenReturn(room);
        when(room.getName()).thenReturn(roomName);

        // When
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        verify(fireEventWebhookService).processParticipantDisconnected(metadata, roomName);
        verify(mediaRecordCommandService, never()).saveRecording(anyString(), anyString());
    }

    @Test
    @DisplayName("라우팅 성공: participant_left 이벤트도 동일하게 FireEventWebhookService로 위임한다")
    void handleWebhookEvent_ParticipantLeft() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";
        String metadata = "{\"type\":\"PUBLISHER\"}";
        String roomName = "fire_event_101";

        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        LivekitModels.ParticipantInfo participant = mock(LivekitModels.ParticipantInfo.class);
        LivekitModels.Room room = mock(LivekitModels.Room.class);

        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        when(event.getEvent()).thenReturn("participant_left"); // left 이벤트

        when(event.hasParticipant()).thenReturn(true);
        when(event.getParticipant()).thenReturn(participant);
        when(participant.getMetadata()).thenReturn(metadata);

        when(event.getRoom()).thenReturn(room);
        when(room.getName()).thenReturn(roomName);

        // When
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        verify(fireEventWebhookService).processParticipantDisconnected(metadata, roomName);
    }

    // --- Egress Ended Tests ---

    @Test
    @DisplayName("라우팅 성공: egress_ended (성공) 이벤트는 MediaRecordCommandService로 위임한다")
    void handleWebhookEvent_EgressEnded_Complete() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";
        String roomName = "fire_event_101";
        String s3Url = "s3://bucket/file.mp4";

        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        LivekitEgress.EgressInfo egressInfo = mock(LivekitEgress.EgressInfo.class);
        LivekitEgress.FileInfo fileInfo = mock(LivekitEgress.FileInfo.class);

        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        when(event.getEvent()).thenReturn("egress_ended");

        when(event.hasEgressInfo()).thenReturn(true);
        when(event.getEgressInfo()).thenReturn(egressInfo);
        when(egressInfo.getStatus()).thenReturn(LivekitEgress.EgressStatus.EGRESS_COMPLETE);
        when(egressInfo.getRoomName()).thenReturn(roomName);

        when(egressInfo.hasFile()).thenReturn(true);
        when(egressInfo.getFile()).thenReturn(fileInfo);
        when(fileInfo.getLocation()).thenReturn(s3Url);

        // When
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        verify(mediaRecordCommandService).saveRecording(roomName, s3Url);
        verify(fireEventWebhookService, never()).processParticipantJoined(anyString());
    }

    @Test
    @DisplayName("무시: egress_ended (실패) 이벤트는 저장 로직을 호출하지 않는다")
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
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        verify(mediaRecordCommandService, never()).saveRecording(anyString(), anyString());
    }

    // --- Exception & Validation Tests ---

    @Test
    @DisplayName("실패: Webhook 서명 검증 실패 시 예외를 던진다")
    void handleWebhookEvent_ValidationFailed() {
        // Given
        String body = "invalid-body";
        String authHeader = "invalid-token";

        when(webhookReceiver.receive(body, authHeader)).thenThrow(new RuntimeException("Invalid signature"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                seperationService.handleWebhookEvent(body, authHeader)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.LIVEKIT_SERVER_ERROR);
    }

    @Test
    @DisplayName("무시: 처리하지 않는 이벤트 타입은 무시한다")
    void handleWebhookEvent_IgnoredEvent() {
        // Given
        String body = "dummy-body";
        String authHeader = "dummy-auth";

        LivekitWebhook.WebhookEvent event = mock(LivekitWebhook.WebhookEvent.class);
        when(webhookReceiver.receive(body, authHeader)).thenReturn(event);
        when(event.getEvent()).thenReturn("room_started");

        // When
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        verifyNoInteractions(fireEventWebhookService);
        verifyNoInteractions(mediaRecordCommandService);
    }
}
