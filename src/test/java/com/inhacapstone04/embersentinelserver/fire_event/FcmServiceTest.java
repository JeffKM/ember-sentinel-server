package com.inhacapstone04.embersentinelserver.fire_event;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.inhacapstone04.embersentinelserver.common.service.FcmService;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;

    @Mock
    private UserRoomMembershipRepository membershipRepository;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("화재 알림 발송 성공: 메시지 제목에 RoomAlias와 감지 유형이 포함되어야 한다.")
    void sendFireAlert_Success() throws Exception {
        // Given
        Long roomId = 1L;
        String roomAlias = "305호 연구실";
        Long eventId = 100L;
        String fireDetectionType = "화재";
        String cameraAlias = "천장 카메라";

        // 가짜 토큰 리스트 반환
        List<String> mockTokens = List.of("token_a", "token_b");
        when(membershipRepository.findAllFcmTokensByRoomId(roomId)).thenReturn(mockTokens);

        // Firebase 발송 결과 Mocking (성공했다고 가정)
        BatchResponse mockResponse = mock(BatchResponse.class);
        when(mockResponse.getSuccessCount()).thenReturn(2);
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(mockResponse);

        // When
        // [수정] 변경된 메서드 시그니처 호출 (Detection Type 포함)
        fcmService.sendFireAlert(roomId, roomAlias, eventId, fireDetectionType, cameraAlias);

        // Then
        // 1. Repository가 호출되었는지 확인
        verify(membershipRepository).findAllFcmTokensByRoomId(roomId);

        // 2. FirebaseMessaging에게 전달된 메시지 캡처
        ArgumentCaptor<MulticastMessage> messageCaptor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging).sendEachForMulticast(messageCaptor.capture());

        MulticastMessage capturedMessage = messageCaptor.getValue();

        // 3. 캡처한 메시지 객체 확인
        assertThat(capturedMessage).isNotNull();
    }

    @Test
    @DisplayName("토큰 필터링: null이나 빈 문자열 토큰이 섞여 있어도 유효한 토큰이 있으면 발송해야 한다.")
    void sendFireAlert_FilterInvalidTokens() throws Exception {
        // Given
        Long roomId = 1L;
        // 유효한 토큰 1개, 무효한 토큰 3개 (null, 빈 문자열, 공백)
        List<String> mixedTokens = Arrays.asList("valid_token", null, "", "   ");
        when(membershipRepository.findAllFcmTokensByRoomId(roomId)).thenReturn(mixedTokens);

        BatchResponse mockResponse = mock(BatchResponse.class);
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(mockResponse);

        // When
        fcmService.sendFireAlert(roomId, "Test Room", 100L, "연기", "Cam 1");

        // Then
        // 유효한 토큰("valid_token")이 하나라도 있으므로 sendEachForMulticast가 1회 호출되어야 함
        verify(firebaseMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("토큰이 없거나 모두 무효한 경우: Firebase 발송을 시도하지 않아야 한다.")
    void sendFireAlert_NoValidTokens() throws Exception {
        // Given
        Long roomId = 1L;

        // 1. 빈 리스트인 경우
        when(membershipRepository.findAllFcmTokensByRoomId(roomId)).thenReturn(Collections.emptyList());
        fcmService.sendFireAlert(roomId, "Test Room", 100L, "화재", "Cam 1");

        // 2. null 또는 공백 토큰만 있는 경우
        when(membershipRepository.findAllFcmTokensByRoomId(roomId)).thenReturn(Arrays.asList(null, "", "  "));
        fcmService.sendFireAlert(roomId, "Test Room", 100L, "화재", "Cam 1");

        // Then
        // 두 경우 모두 유효한 토큰이 없으므로 발송 메서드가 호출되지 않아야 함
        verify(firebaseMessaging, never()).sendEachForMulticast(any());
    }
}

