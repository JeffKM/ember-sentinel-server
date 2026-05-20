package com.inhacapstone04.embersentinelserver.fire_event;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.FireEventSimpleDTO;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventDetailResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventWatchResponse;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireCause;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventQueryService;
import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaRecordRepository;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.support.IntegrationTestSupport;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("FireEventQueryService 통합 테스트")
class FireEventQueryServiceTest extends IntegrationTestSupport {

    @Autowired private FireEventQueryService fireEventQueryService;
    @Autowired private MediaRecordRepository mediaRecordRepository;
    @Autowired private EntityManager em;

    private User memberUser;
    private User strangerUser;
    private Room roomA;
    private Room roomB;
    private CameraEdge cameraA; // Room A 소속
    private CameraEdge cameraB; // Room B 소속
    private FireEvent eventA;

    @BeforeEach
    void setUp() {
        // 1. 유저 생성
        memberUser = createUser("member@test.com", "MemberUser");
        strangerUser = createUser("stranger@test.com", "StrangerUser");

        // 2. 빌딩 및 방 생성
        Building building = createBuilding("Test Building");
        roomA = createRoom("Room A", building);
        roomB = createRoom("Room B", building);

        // 3. 멤버십 설정 (memberUser는 roomA의 멤버)
        createMembership(memberUser, roomA, MembershipRole.VIEWER);

        // 4. 카메라 생성
        cameraA = createCamera(roomA, "uuid-room-a", "Cam A");
        cameraB = createCamera(roomB, "uuid-room-b", "Cam B");

        // 5. 화재 이벤트 생성 (Room A의 Camera A에서 발생)
        eventA = createFireEvent(cameraA);

        // 6. 연관 데이터 생성 (Stream, Record)
        createMediaStream(eventA, "stream-key-a");
        createMediaRecord(eventA, "s3/path/record-a.mp4");

        em.flush();
        em.clear();
    }

    // --- getFireEventDetail Tests ---

    @Test
    @DisplayName("상세 조회 성공: 멤버가 자신의 방에서 발생한 이벤트를 조회")
    void getFireEventDetail_Success() {
        // Given
        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long eventId = eventA.getId();

        // When
        FireEventDetailResponse response = fireEventQueryService.getFireEventDetail(userId, roomId, eventId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(eventId);
        assertThat(response.cameraInfo().deviceUuid()).isEqualTo(cameraA.getDeviceUuid());
        if (response.streamInfo() != null) {
            assertThat(response.streamInfo().livekitRoomName()).isEqualTo("stream-key-a");
        }
    }

    @Test
    @DisplayName("상세 조회 실패: 요청자가 방의 멤버가 아님 (403 FORBIDDEN)")
    void getFireEventDetail_Fail_NotMember() {
        Long userId = strangerUser.getId();
        Long roomId = roomA.getId();
        Long eventId = eventA.getId();

        assertThatThrownBy(() -> fireEventQueryService.getFireEventDetail(userId, roomId, eventId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    @Test
    @DisplayName("상세 조회 실패: 이벤트가 존재하지 않음 (404 NOT_FOUND)")
    void getFireEventDetail_Fail_EventNotFound() {
        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long nonExistentEventId = 9999L;

        assertThatThrownBy(() -> fireEventQueryService.getFireEventDetail(userId, roomId, nonExistentEventId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    @Test
    @DisplayName("상세 조회 실패: 이벤트는 존재하지만 요청한 방에 속하지 않음 (404 NOT_FOUND - 보안상)")
    void getFireEventDetail_Fail_EventInDifferentRoom() {
        FireEvent eventB = createFireEvent(cameraB);
        em.flush();
        em.clear();

        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long eventIdInRoomB = eventB.getId();

        assertThatThrownBy(() -> fireEventQueryService.getFireEventDetail(userId, roomId, eventIdInRoomB))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    // --- getFireEventList Tests ---

    @Test
    @DisplayName("목록 조회 성공: 특정 카메라의 화재 이벤트 목록 페이징 조회")
    void getFireEventList_Success() {
        createFireEvent(cameraA);
        createFireEvent(cameraA);
        em.flush();
        em.clear();

        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long cameraEdgeId = cameraA.getId();
        Pageable pageable = PageRequest.of(0, 10);

        PageResponse<FireEventSimpleDTO> response = fireEventQueryService.getFireEventList(userId, roomId, cameraEdgeId, pageable);

        // setUp에서 1개 + 여기서 2개 = 총 3개
        assertThat(response.totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("목록 조회 실패: 요청자가 방의 멤버가 아님 (403 FORBIDDEN)")
    void getFireEventList_Fail_NotMember() {
        Long userId = strangerUser.getId();
        Long roomId = roomA.getId();
        Long cameraEdgeId = cameraA.getId();
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> fireEventQueryService.getFireEventList(userId, roomId, cameraEdgeId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    @Test
    @DisplayName("목록 조회 실패: 카메라가 존재하지 않음 (404 NOT_FOUND)")
    void getFireEventList_Fail_CameraNotFound() {
        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long nonExistentCameraId = 9999L;
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> fireEventQueryService.getFireEventList(userId, roomId, nonExistentCameraId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    @Test
    @DisplayName("목록 조회 실패: 카메라가 요청한 방에 속하지 않음 (404 NOT_FOUND)")
    void getFireEventList_Fail_CameraInDifferentRoom() {
        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long cameraEdgeIdInRoomB = cameraB.getId();
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> fireEventQueryService.getFireEventList(userId, roomId, cameraEdgeIdInRoomB, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    // --- getFireEventWatchToken Tests (신규 추가) ---

    @Test
    @DisplayName("시청 토큰 발급 성공: 멤버가 화재 상황 시청을 위한 토큰 요청")
    void getFireEventWatchToken_Success() {
        // Given
        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long eventId = eventA.getId();

        // When
        FireEventWatchResponse response = fireEventQueryService.getFireEventWatchToken(userId, roomId, eventId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.livekitRoomName()).isEqualTo("stream-key-a"); // setUp에서 설정한 streamKey
        assertThat(response.subscriberToken()).isNotNull().isNotEmpty();
        // [수정됨] DTO에 추가된 fireEventId 검증
        assertThat(response.fireEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("시청 토큰 발급 실패: 요청자가 방의 멤버가 아님 (403 FORBIDDEN)")
    void getFireEventWatchToken_Fail_NotMember() {
        // Given
        Long userId = strangerUser.getId();
        Long roomId = roomA.getId();
        Long eventId = eventA.getId();

        // When & Then
        assertThatThrownBy(() -> fireEventQueryService.getFireEventWatchToken(userId, roomId, eventId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    @Test
    @DisplayName("시청 토큰 발급 실패: 이벤트가 존재하지 않음 (404 NOT_FOUND)")
    void getFireEventWatchToken_Fail_EventNotFound() {
        // Given
        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long nonExistentEventId = 9999L;

        // When & Then
        assertThatThrownBy(() -> fireEventQueryService.getFireEventWatchToken(userId, roomId, nonExistentEventId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    @Test
    @DisplayName("시청 토큰 발급 실패: 이벤트가 다른 방에 속함 (404 NOT_FOUND)")
    void getFireEventWatchToken_Fail_EventInDifferentRoom() {
        // Given
        // Room B의 카메라 B에서 발생한 이벤트 생성
        FireEvent eventB = createFireEvent(cameraB);
        createMediaStream(eventB, "stream-key-b");
        em.flush();
        em.clear();

        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long eventIdInRoomB = eventB.getId();

        // When & Then
        assertThatThrownBy(() -> fireEventQueryService.getFireEventWatchToken(userId, roomId, eventIdInRoomB))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    @Test
    @DisplayName("시청 토큰 발급 실패: 이벤트에 대한 미디어 스트림 정보가 없음 (404 NOT_FOUND)")
    void getFireEventWatchToken_Fail_NoMediaStream() {
        // Given
        // 미디어 스트림이 없는 이벤트 생성
        FireEvent eventNoStream = createFireEvent(cameraA);
        em.flush();
        em.clear();

        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long eventId = eventNoStream.getId();

        // When & Then
        assertThatThrownBy(() -> fireEventQueryService.getFireEventWatchToken(userId, roomId, eventId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    // --- 로컬 헬퍼 (부모와 시그니처가 다른 것만 유지) ---

    private Room createRoom(String alias, Building building) {
        return createRoom(alias, null, null, building);
    }

    // 이 테스트 전용: FireCause/RiskRank 포함
    @Override
    protected FireEvent createFireEvent(CameraEdge camera) {
        FireEvent event = new FireEvent();
        event.setCameraEdge(camera);
        event.setDetectionType(com.inhacapstone04.embersentinelserver.fire_event.entity.DetectionType.FIRE);
        event.setFireCause(FireCause.기타);
        event.setRiskRank(1L);
        return fireEventRepository.save(event);
    }

    private MediaStream createMediaStream(FireEvent event, String streamKey) {
        MediaStream stream = new MediaStream();
        stream.setFireEvent(event);
        stream.setLivekitRoomName(streamKey);
        stream.setStreamingStatus(StreamingStatus.ENDED);
        return mediaStreamRepository.save(stream);
    }

    private MediaRecord createMediaRecord(FireEvent event, String path) {
        MediaRecord record = new MediaRecord();
        record.setFireEvent(event);
        record.setS3BucketPath(path);
        return mediaRecordRepository.save(record);
    }
}
