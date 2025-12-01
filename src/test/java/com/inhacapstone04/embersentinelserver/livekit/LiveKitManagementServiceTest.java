package com.inhacapstone04.embersentinelserver.livekit;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.LiveKitManagementService;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveKitManagementServiceTest {

    @InjectMocks
    private LiveKitManagementService liveKitManagementService;

    @Mock
    private RoomServiceClient roomServiceClient;

    @Mock
    private EgressServiceClient egressServiceClient;

    // Retrofit Call 객체 Mocking
    @Mock private Call<LivekitModels.Room> roomCall;
    @Mock private Call<LivekitEgress.EgressInfo> egressCall;
    @Mock private Call<Void> voidCall;

    private final String ROOM_NAME = "fire_event_101";
    private final String TEST_BUCKET = "test-bucket";
    private final String TEST_REGION = "us-test-1";

    @BeforeEach
    void setUp() {
        // @Value로 주입되는 private 필드에 테스트용 값을 Reflection으로 주입
        ReflectionTestUtils.setField(liveKitManagementService, "s3BucketName", TEST_BUCKET);
        ReflectionTestUtils.setField(liveKitManagementService, "awsRegion", TEST_REGION);
    }

    // --- createRoomAndStartEgress Tests ---

    @Test
    @DisplayName("성공: Room 생성 후, S3 설정이 포함된 Egress 요청을 보내야 한다.")
    void createRoomAndStartEgress_Success() throws IOException {
        // Given
        // 1. Room 생성 Mock
        when(roomServiceClient.createRoom(ROOM_NAME)).thenReturn(roomCall);
        when(roomCall.execute()).thenReturn(Response.success(LivekitModels.Room.newBuilder().setName(ROOM_NAME).build()));

        // 2. Egress 시작 Mock
        LivekitEgress.EgressInfo egressInfo = LivekitEgress.EgressInfo.newBuilder().setEgressId("egress_123").build();

        // startRoomCompositeEgress 호출 시 인자 캡처를 위해 any() 사용
        when(egressServiceClient.startRoomCompositeEgress(
                eq(ROOM_NAME),
                any(LivekitEgress.EncodedFileOutput.class),
                eq("single-speaker"))
        ).thenReturn(egressCall);

        when(egressCall.execute()).thenReturn(Response.success(egressInfo));

        // When
        liveKitManagementService.createRoomAndStartEgress(ROOM_NAME);

        // Then
        verify(roomServiceClient).createRoom(ROOM_NAME);

        // [핵심] Egress 요청 시 전달된 EncodedFileOutput 객체를 캡처하여 검증
        ArgumentCaptor<LivekitEgress.EncodedFileOutput> outputCaptor = ArgumentCaptor.forClass(LivekitEgress.EncodedFileOutput.class);

        verify(egressServiceClient).startRoomCompositeEgress(
                eq(ROOM_NAME),
                outputCaptor.capture(), // 인자 가로채기
                eq("single-speaker")
        );

        LivekitEgress.EncodedFileOutput capturedOutput = outputCaptor.getValue();

        // 1. 파일 경로 확인
        assertThat(capturedOutput.getFilepath()).isEqualTo("recordings/" + ROOM_NAME + ".mp4");

        // 2. S3 설정이 포함되었는지 확인
        assertThat(capturedOutput.hasS3()).isTrue();
        assertThat(capturedOutput.getS3().getBucket()).isEqualTo(TEST_BUCKET);
        assertThat(capturedOutput.getS3().getRegion()).isEqualTo(TEST_REGION);
    }

    @Test
    @DisplayName("실패: Room 생성이 실패하면 예외를 던지고 Egress를 시도하지 않는다.")
    void createRoomAndStartEgress_Fail_RoomCreation() throws IOException {
        // Given
        when(roomServiceClient.createRoom(ROOM_NAME)).thenReturn(roomCall);

        ResponseBody errorBody = ResponseBody.create(MediaType.parse("text/plain"), "Room already exists");
        when(roomCall.execute()).thenReturn(Response.error(400, errorBody));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                liveKitManagementService.createRoomAndStartEgress(ROOM_NAME)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.LIVEKIT_SERVER_ERROR);

        // Egress 요청은 호출되지 않아야 함
        verify(egressServiceClient, never()).startRoomCompositeEgress(
                anyString(),
                any(LivekitEgress.EncodedFileOutput.class),
                anyString()
        );
    }

    @Test
    @DisplayName("실패: Room은 생성되었으나 Egress 시작이 실패하면 예외를 던진다.")
    void createRoomAndStartEgress_Fail_EgressStart() throws IOException {
        // Given
        // 1. Room 생성 성공
        when(roomServiceClient.createRoom(ROOM_NAME)).thenReturn(roomCall);
        when(roomCall.execute()).thenReturn(Response.success(LivekitModels.Room.getDefaultInstance()));

        // 2. Egress 실패 시뮬레이션
        when(egressServiceClient.startRoomCompositeEgress(
                anyString(),
                any(LivekitEgress.EncodedFileOutput.class),
                anyString())
        ).thenReturn(egressCall);

        ResponseBody errorBody = ResponseBody.create(MediaType.parse("text/plain"), "Egress limit reached");
        when(egressCall.execute()).thenReturn(Response.error(500, errorBody));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                liveKitManagementService.createRoomAndStartEgress(ROOM_NAME)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.LIVEKIT_SERVER_ERROR);
    }

    @Test
    @DisplayName("실패: 네트워크 오류(IOException) 발생 시 예외를 던진다.")
    void createRoomAndStartEgress_Fail_NetworkError() throws IOException {
        // Given
        when(roomServiceClient.createRoom(ROOM_NAME)).thenReturn(roomCall);
        when(roomCall.execute()).thenThrow(new IOException("Connection refused"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                liveKitManagementService.createRoomAndStartEgress(ROOM_NAME)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.LIVEKIT_SERVER_ERROR);
    }

    // --- deleteRoom Tests ---

    @Test
    @DisplayName("성공: 방 삭제 요청이 성공한다.")
    void deleteRoom_Success() throws IOException {
        // Given
        when(roomServiceClient.deleteRoom(ROOM_NAME)).thenReturn(voidCall);
        when(voidCall.execute()).thenReturn(Response.success(null));

        // When
        liveKitManagementService.deleteRoom(ROOM_NAME);

        // Then
        verify(roomServiceClient).deleteRoom(ROOM_NAME);
    }

    @Test
    @DisplayName("실패(무시): 방 삭제 요청이 실패해도 예외를 던지지 않는다.")
    void deleteRoom_Fail_Ignored() throws IOException {
        // Given
        when(roomServiceClient.deleteRoom(ROOM_NAME)).thenReturn(voidCall);

        // 네트워크 오류 발생 시뮬레이션
        when(voidCall.execute()).thenThrow(new IOException("Network timeout"));

        // When & Then
        // 예외가 발생하지 않고 로그만 남기고 종료되어야 함
        assertDoesNotThrow(() -> liveKitManagementService.deleteRoom(ROOM_NAME));

        verify(roomServiceClient).deleteRoom(ROOM_NAME);
    }
}