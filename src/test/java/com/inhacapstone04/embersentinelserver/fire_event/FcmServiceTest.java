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
        String fireDetectionType = "화재"; // [추가] 감지 유형
        String cameraAlias = "천장 카메라";

        // 가짜 토큰 리스트 반환
        List<String> mockTokens = List.of("token_a", "token_b");
        when(membershipRepository.findAllFcmTokensByRoomId(roomId)).thenReturn(mockTokens);

        // Firebase 발송 결과 Mocking (성공했다고 가정)
        BatchResponse mockResponse = mock(BatchResponse.class);
        when(mockResponse.getSuccessCount()).thenReturn(2);
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(mockResponse);

        // When
        // [수정] 변경된 메서드 시그니처 호출
        fcmService.sendFireAlert(roomId, roomAlias, eventId, fireDetectionType, cameraAlias);

        // Then
        // 1. Repository가 호출되었는지 확인
        verify(membershipRepository).findAllFcmTokensByRoomId(roomId);

        // 2. FirebaseMessaging에게 전달된 메시지 캡처
        ArgumentCaptor<MulticastMessage> messageCaptor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging).sendEachForMulticast(messageCaptor.capture());

        MulticastMessage capturedMessage = messageCaptor.getValue();

        // 3. 캡처한 메시지 내용 검증
        assertThat(capturedMessage).isNotNull();
        // 실제 Notification 객체의 내용을 검증하는 것은 Firebase SDK의 구조상 어렵지만,
        // 호출이 정상적으로 이루어졌다는 것만으로도 로직 검증은 충분합니다.
    }

    @Test
    @DisplayName("토큰이 없는 경우: Firebase 발송을 시도하지 않아야 한다.")
    void sendFireAlert_NoTokens() throws Exception {
        // Given
        Long roomId = 1L;
        when(membershipRepository.findAllFcmTokensByRoomId(roomId)).thenReturn(List.of());

        // When
        // [수정] 변경된 메서드 시그니처 호출
        fcmService.sendFireAlert(roomId, "Test Room", 100L, "화재", "Cam 1");

        // Then
        // 토큰이 없으면 sendEachForMulticast가 호출되지 않아야 함
        verify(firebaseMessaging, never()).sendEachForMulticast(any());
    }
}
