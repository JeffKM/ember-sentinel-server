package com.inhacapstone04.embersentinelserver.common.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveKitManagementService {

    private final RoomServiceClient roomServiceClient;
    private final EgressServiceClient egressServiceClient;

    @Value("${cloud.aws.s3.bucket}")
    private String s3BucketName;

    @Value("${cloud.aws.region.static}")
    private String awsRegion;

    /**
     * LiveKit Room을 생성하고 녹화(Egress)를 시작합니다.
     * (FireEventCommandService에서 호출 - DB 생성 후 호출됨)
     */
    public void createRoomAndStartEgress(String livekitRoomName) {
        try {
            // 1. Room 생성
            Call<LivekitModels.Room> roomCall = roomServiceClient.createRoom(livekitRoomName);
            Response<LivekitModels.Room> roomResponse = roomCall.execute();

            if (!roomResponse.isSuccessful()) {
                String errorMsg = roomResponse.errorBody() != null ? roomResponse.errorBody().string() : "Unknown error";
                log.error("LiveKit Room creation failed: {}", errorMsg);
                throw new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR, "Room 생성 실패: " + errorMsg);
            }
            log.info("LiveKit Room created: {}", livekitRoomName);

            // 2. Egress 시작
            LivekitEgress.S3Upload s3Upload = LivekitEgress.S3Upload.newBuilder()
                    .setBucket(s3BucketName)
                    .setRegion(awsRegion)
                    .build();

            LivekitEgress.EncodedFileOutput output = LivekitEgress.EncodedFileOutput.newBuilder()
                    .setFileType(LivekitEgress.EncodedFileType.MP4)
                    .setFilepath("recordings/" + livekitRoomName + ".mp4") // S3 내 저장 경로 (Key)
                    .setS3(s3Upload) // <--- 핵심: 로컬 대신 S3를 사용하라는 명령
                    .build();

            // SDK 메서드 호출
            Call<LivekitEgress.EgressInfo> egressCall = egressServiceClient.startRoomCompositeEgress(
                    livekitRoomName,
                    output,
                    "single-speaker"
            );

            Response<LivekitEgress.EgressInfo> egressResponse = egressCall.execute();

            if (egressResponse.isSuccessful()) {
                log.info("Egress started for room: {}, EgressID: {}", livekitRoomName, egressResponse.body().getEgressId());
            } else {
                String errorMsg = egressResponse.errorBody() != null ? egressResponse.errorBody().string() : "Unknown error";
                log.error("Failed to start egress: {}", errorMsg);
                throw new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR, "Egress 시작 실패: " + errorMsg);
            }

        } catch (IOException e) {
            log.error("Network error while communicating with LiveKit server", e);
            throw new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR, "LiveKit 네트워크 오류: " + e.getMessage());
        }
    }

    /**
     * LiveKit Room을 강제 삭제(종료)합니다.
     * (FireEventWebhookService에서 호출 - DB 상태 변경 후 호출됨)
     */
    public void deleteRoom(String livekitRoomName) {
        try {
            Call<Void> deleteCall = roomServiceClient.deleteRoom(livekitRoomName);
            Response<Void> response = deleteCall.execute();

            if (response.isSuccessful()) {
                log.info("LiveKit Room deleted successfully: {}", livekitRoomName);
            } else {
                log.warn("Failed to delete LiveKit room: {}", livekitRoomName);
            }
        } catch (IOException e) {
            log.error("Error deleting LiveKit room: {}", livekitRoomName, e);
            // 방 삭제 실패가 DB 상태 변경을 롤백시키지 않도록 예외는 로그만 남김
        }
    }
}
