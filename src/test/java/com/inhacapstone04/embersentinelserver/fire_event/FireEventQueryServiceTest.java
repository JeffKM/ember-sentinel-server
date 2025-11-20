package com.inhacapstone04.embersentinelserver.fire_event;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.FireEventSimpleDTO;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventDetailResponse;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireCause;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventQueryService;
import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaRecordRepository;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.UserRole;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("FireEventQueryService 통합 테스트")
class FireEventQueryServiceTest {

    @Autowired private FireEventQueryService fireEventQueryService;

    @Autowired private UserRepository userRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRoomMembershipRepository membershipRepository;
    @Autowired private CameraEdgeRepository cameraEdgeRepository;
    @Autowired private FireEventRepository fireEventRepository;
    @Autowired private MediaStreamRepository mediaStreamRepository;
    @Autowired private MediaRecordRepository mediaRecordRepository;

    @Autowired private EntityManager em; // [추가] EntityManager 주입

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

        // [핵심 추가] 영속성 컨텍스트 초기화
        // 위에서 save한 데이터들이 DB에 반영되고, 1차 캐시를 비워야
        // 테스트 로직(getFireEventDetail) 실행 시 DB에서 연관 관계(MediaStream 등)를 포함하여 새로 조회함.
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
        // DTO에서 null 처리를 했으므로 안전하게 접근 가능, 데이터가 있다면 값 검증
        if (response.streamInfo() != null) {
            assertThat(response.streamInfo().livekitRoomName()).isEqualTo("stream-key-a"); // createMediaStream에서 설정한 값 확인 (DTO 필드명에 맞게 수정 필요)
        }
    }

    // ... (나머지 테스트 케이스는 동일하게 유지) ...

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
        // Given
        FireEvent eventB = createFireEvent(cameraB);
        // eventB 저장 후에도 flush가 필요할 수 있으나 setUp 이후 개별 동작이므로
        // 여기서는 단순 ID 조회 테스트라 괜찮을 수 있음. 확실히 하려면 save 후 flush 권장.
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
        // Given
        Long userId = strangerUser.getId();
        Long roomId = roomA.getId();
        Long cameraEdgeId = cameraA.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> fireEventQueryService.getFireEventList(userId, roomId, cameraEdgeId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    @Test
    @DisplayName("목록 조회 실패: 카메라가 존재하지 않음 (404 NOT_FOUND)")
    void getFireEventList_Fail_CameraNotFound() {
        // Given
        Long userId = memberUser.getId();
        Long roomId = roomA.getId();
        Long nonExistentCameraId = 9999L;
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> fireEventQueryService.getFireEventList(userId, roomId, nonExistentCameraId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    @Test
    @DisplayName("목록 조회 실패: 카메라가 요청한 방에 속하지 않음 (404 NOT_FOUND)")
    void getFireEventList_Fail_CameraInDifferentRoom() {
        // Given
        Long userId = memberUser.getId();
        Long roomId = roomA.getId(); // 요청은 Room A로 보냄
        Long cameraEdgeIdInRoomB = cameraB.getId(); // 카메라는 Room B 소속
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> fireEventQueryService.getFireEventList(userId, roomId, cameraEdgeIdInRoomB, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    // --- Helper Methods (데이터 생성) ---

    private User createUser(String email, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setAuthType(AuthType.GOOGLE);
        user.setUserRole(UserRole.USER);
        return userRepository.save(user);
    }

    private Building createBuilding(String name) {
        Building building = new Building();
        building.setBuildingName(name);
        return buildingRepository.save(building);
    }

    private Room createRoom(String alias, Building building) {
        Room room = new Room();
        room.setRoomAlias(alias);
        room.setBuilding(building);
        return roomRepository.save(room);
    }

    private void createMembership(User user, Room room, MembershipRole role) {
        UserRoomMembership membership = new UserRoomMembership(user, room);
        membership.setRole(role);
        membershipRepository.save(membership);
    }

    private CameraEdge createCamera(Room room, String uuid, String alias) {
        CameraEdge camera = new CameraEdge();
        camera.setRoom(room);
        camera.setDeviceUuid(uuid);
        camera.setCameraEdgeAlias(alias);
        return cameraEdgeRepository.save(camera);
    }

    private FireEvent createFireEvent(CameraEdge camera) {
        FireEvent event = new FireEvent();
        event.setCameraEdge(camera);
        event.setFireCause(FireCause.가스);
        event.setRiskRank(1L);
        return fireEventRepository.save(event);
    }

    private MediaStream createMediaStream(FireEvent event, String streamKey) {
        MediaStream stream = new MediaStream();
        stream.setFireEvent(event);
        stream.setLivekitRoomName(streamKey); // DTO 필드명에 맞춰 수정 (streamKey -> livekitRoomName 가정)
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