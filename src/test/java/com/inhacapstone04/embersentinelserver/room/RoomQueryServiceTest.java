package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.camera_edge.dto.CameraEdgeWithIsFireDTO;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.room.dto.response.RoomDetailResponse;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import com.inhacapstone04.embersentinelserver.room.dto.response.RoomDashboardResponse;
import com.inhacapstone04.embersentinelserver.room.dto.RoomStatisticsDTO;
import com.inhacapstone04.embersentinelserver.room.dto.response.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import com.inhacapstone04.embersentinelserver.room.service.RoomQueryService;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.UserRole;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional // 테스트 후 DB 롤백
@DisplayName("RoomQueryService 통합 테스트")
class RoomQueryServiceTest {

    @Autowired
    private RoomQueryService roomQueryService;

    // --- 테스트 데이터 세팅을 위한 Repository ---
    @Autowired private UserRepository userRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRoomMembershipRepository membershipRepository;
    @Autowired private CameraEdgeRepository cameraEdgeRepository;
    @Autowired private FireEventRepository fireEventRepository;
    @Autowired private MediaStreamRepository mediaStreamRepository;

    private User testUser1;
    private User testUser2;
    private Room roomA, roomB, roomC_OtherUser;
    private CameraEdge camA1, camA2, camB1;
    private FireEvent eventA_Live;

    /**
     * 테스트 데이터 세팅:
     * - User1: roomA (EDITOR), roomB (EDITOR)
     * - User2: roomC (EDITOR)
     *
     * - roomA: 카메라 2대 (camA1, camA2), LIVE 이벤트 1개 (camA1), ENDED 이벤트 1개 (camA2)
     * - roomB: 카메라 1대 (camB1), LIVE 이벤트 0개
     * - roomC: (User2의 방)
     */
    @BeforeEach
    void setUp() {
        // 1. 유저 생성
        testUser1 = createUser("user1@test.com", "User1");
        testUser2 = createUser("user2@test.com", "User2");

        // 2. 빌딩 생성
        Building building = createBuilding("하이테크센터");

        // 3. 방 생성 (Floor, RoomNumber 설정 포함)
        roomA = createRoom("정보보안연구실", "3", "305", building);
        roomB = createRoom("지능형보안연구실", "11", "1105", building);
        roomC_OtherUser = createRoom("서버실", "5", "501", building);

        // 4. 멤버십 연결 (EDITOR로 통일)
        createMembership(testUser1, roomA, MembershipRole.EDITOR);
        createMembership(testUser1, roomB, MembershipRole.EDITOR);
        createMembership(testUser2, roomC_OtherUser, MembershipRole.EDITOR);

        // 5. 카메라 생성
        camA1 = createCamera(roomA, "cam-A1-uuid", "A1-천장");
        camA2 = createCamera(roomA, "cam-A2-uuid", "A2-입구"); // roomA (카메라 2대)
        camB1 = createCamera(roomB, "cam-B1-uuid", "B1-서버랙"); // roomB (카메라 1대)

        // 6. 화재 이벤트 및 미디어 스트림 생성
        // roomA - camA1: LIVE 이벤트 1개 -> DTO 변환 시 isFireOccurring = true여야 함
        eventA_Live = createFireEvent(camA1);
        createMediaStream(eventA_Live, StreamingStatus.LIVE);

        // roomA - camA2: ENDED 이벤트 1개 -> DTO 변환 시 isFireOccurring = false여야 함
        FireEvent eventA_Ended = createFireEvent(camA2);
        createMediaStream(eventA_Ended, StreamingStatus.ENDED);
    }

    // --- getMyRooms (페이징) 테스트 ---

    @Test
    @DisplayName("내 방 목록 조회 (페이징) 성공")
    void getMyRooms_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 5); // 0번 페이지, 5개씩

        // when
        PageResponse<SingleRoomResponse> response = roomQueryService.getMyRooms(testUser1.getId(), pageable);

        // then
        assertThat(response.pageNumber()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(2); // roomA, roomB
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).roomAlias()).isEqualTo(roomA.getRoomAlias());
    }

    @Test
    @DisplayName("내 방 목록 페이징 처리 (1페이지 1개씩)")
    void getMyRooms_Paging() {
        // given
        Pageable page1 = PageRequest.of(0, 1);
        Pageable page2 = PageRequest.of(1, 1);

        // when
        PageResponse<SingleRoomResponse> response1 = roomQueryService.getMyRooms(testUser1.getId(), page1);
        PageResponse<SingleRoomResponse> response2 = roomQueryService.getMyRooms(testUser1.getId(), page2);

        // then
        assertThat(response1.totalPages()).isEqualTo(2);
        assertThat(response1.content()).hasSize(1);
        assertThat(response1.content().get(0).roomId()).isEqualTo(roomA.getId());
        assertThat(response2.content()).hasSize(1);
        assertThat(response2.content().get(0).roomId()).isEqualTo(roomB.getId());
    }

    // --- getRoomStatistics (대시보드) 테스트 ---

    @Test
    @DisplayName("대시보드 통계 조회 성공 (Room 2개)")
    void getRoomStatistics_Success() {
        // given
        List<Long> requestedIds = List.of(roomA.getId(), roomB.getId());

        // when
        RoomDashboardResponse response = roomQueryService.getRoomStatistics(testUser1.getId(), requestedIds);

        // then (총계)
        assertThat(response.totalRoomCount()).isEqualTo(2);
        assertThat(response.totalCameraCount()).isEqualTo(3); // A(2) + B(1) = 3
        assertThat(response.liveStreamCount()).isEqualTo(1); // A(1) + B(0) = 1

        // then (RoomA 상세)
        RoomStatisticsDTO roomAStats = findStats(response, roomA.getId());
        assertThat(roomAStats.cameraCountPerRoom()).isEqualTo(2);
        assertThat(roomAStats.fireEventCountPerRoom()).isEqualTo(1); // LIVE 1개

        // then (RoomB 상세)
        RoomStatisticsDTO roomBStats = findStats(response, roomB.getId());
        assertThat(roomBStats.cameraCountPerRoom()).isEqualTo(1);
        assertThat(roomBStats.fireEventCountPerRoom()).isEqualTo(0); // LIVE 0개
    }

    @Test
    @DisplayName("대시보드 통계 조회 실패 (권한 없는 Room 포함 시 FORBIDDEN)")
    void getRoomStatistics_Fail_Forbidden() {
        // given
        List<Long> requestedIds = List.of(roomA.getId(), roomC_OtherUser.getId());

        // when & then
        assertThatThrownBy(() -> roomQueryService.getRoomStatistics(testUser1.getId(), requestedIds))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    // --- getRoomDetail (상세 조회) 테스트 ---

    @Test
    @DisplayName("Room 상세 조회 성공 (화재 O, 화재 X 카메라 동시 조회)")
    void getRoomDetail_Success() {
        // given
        Long userId = testUser1.getId();
        Long roomId = roomA.getId(); // roomA: camA1(LIVE), camA2(ENDED)

        // when
        RoomDetailResponse response = roomQueryService.getRoomDetail(userId, roomId);

        // then (1. 기본 정보)
        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.roomAlias()).isEqualTo(roomA.getRoomAlias());
        assertThat(response.buildingName()).isEqualTo(roomA.getBuilding().getBuildingName());

        // then (2. 멤버 정보)
        assertThat(response.members()).hasSize(1);
        assertThat(response.members().get(0).userId()).isEqualTo(userId);
        assertThat(response.members().get(0).role()).isEqualTo(MembershipRole.EDITOR.name());

        // then (3. 카메라 및 화재 정보)
        assertThat(response.cameras()).hasSize(2);

        // camA1 (LIVE 이벤트 존재) -> isFireOccurring = true
        // [수정] CameraEdgeWithIsFireDTO 타입 사용
        CameraEdgeWithIsFireDTO camA1_dto = findCamera(response, camA1.getId());
        assertThat(camA1_dto.cameraEdgeAlias()).isEqualTo(camA1.getCameraEdgeAlias());
        assertThat(camA1_dto.isFireOccurring()).isTrue(); // LIVE 상태이므로
        assertThat(camA1_dto.fireEventId()).isEqualTo(eventA_Live.getId()); // LIVE 이벤트 ID

        // [추가됨] 새로 추가된 필드 검증 (Floor, RoomNumber)
        assertThat(camA1_dto.locationFloor()).isEqualTo(roomA.getBuildingLocationFloor());
        assertThat(camA1_dto.roomNumber()).isEqualTo(roomA.getRoomNumber());

        // camA2 (ENDED 이벤트 존재 -> 쿼리 결과 false) -> isFireOccurring = false
        // [수정] CameraEdgeWithIsFireDTO 타입 사용
        CameraEdgeWithIsFireDTO camA2_dto = findCamera(response, camA2.getId());
        assertThat(camA2_dto.cameraEdgeAlias()).isEqualTo(camA2.getCameraEdgeAlias());
        assertThat(camA2_dto.isFireOccurring()).isFalse(); // LIVE가 아니므로
        assertThat(camA2_dto.fireEventId()).isNull(); // LIVE가 아니므로 (생성자 로직에 의해 null)
    }

    @Test
    @DisplayName("Room 상세 조회 실패 (접근 권한 없는 Room 요청 시 FORBIDDEN)")
    void getRoomDetail_Fail_Forbidden() {
        // given
        Long userId = testUser1.getId();
        Long forbiddenRoomId = roomC_OtherUser.getId(); // User2의 방

        // when & then
        assertThatThrownBy(() -> roomQueryService.getRoomDetail(userId, forbiddenRoomId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    @Test
    @DisplayName("Room 상세 조회 실패 (존재하지 않는 Room 요청 시 FORBIDDEN)")
    void getRoomDetail_Fail_NonExistentRoom() {
        // given
        Long userId = testUser1.getId();
        Long nonExistentRoomId = 9999L; // 존재하지 않는 ID

        // when & then
        // (Auth 로직이 먼저 동작하므로 ROOM_NOT_FOUND가 아닌 FORBIDDEN이 발생)
        // *참고: 서비스 로직 순서(Auth vs Find)에 따라 기대하는 에러 코드가 다를 수 있음.
        // 이전에 수정한 로직에 맞춰 NOT_FOUND로 설정.
        assertThatThrownBy(() -> roomQueryService.getRoomDetail(userId, nonExistentRoomId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }


    // --- 테스트용 헬퍼 메서드 ---

    private User createUser(String email, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setUserRole(UserRole.USER);
        user.setAuthType(AuthType.GOOGLE);
        return userRepository.save(user);
    }

    private Building createBuilding(String name) {
        Building building = new Building();
        building.setBuildingName(name);
        return buildingRepository.save(building);
    }

    // Floor와 RoomNumber를 받도록 수정됨
    private Room createRoom(String alias, String floor, String roomNum, Building building) {
        Room room = new Room();
        room.setRoomAlias(alias);
        room.setBuildingLocationFloor(floor);
        room.setRoomNumber(roomNum);
        room.setBuilding(building);
        return roomRepository.save(room);
    }

    private void createMembership(User user, Room room, MembershipRole role) {
        UserRoomMembership membership = new UserRoomMembership(user, room);
        membership.setRole(role); // [수정됨] 널 제약조건 해결
        room.getUserMemberships().add(membership); // <-- 이 줄을 추가하세요.

        membershipRepository.save(membership);
    }

    private CameraEdge createCamera(Room room, String uuid, String alias) {
        CameraEdge camera = new CameraEdge();
        camera.setRoom(room);
        camera.setDeviceUuid(uuid); // (CameraEdge 엔티티에 deviceUuid 필드가 있다고 가정)
        camera.setCameraEdgeAlias(alias);
        return cameraEdgeRepository.save(camera);
    }

    private FireEvent createFireEvent(CameraEdge camera) {
        FireEvent event = new FireEvent();
        event.setCameraEdge(camera);
        return fireEventRepository.save(event);
    }

    private void createMediaStream(FireEvent event, StreamingStatus status) {
        MediaStream stream = new MediaStream();
        stream.setFireEvent(event);
        stream.setStreamingStatus(status);
        mediaStreamRepository.save(stream);
    }

    // [헬퍼] 대시보드 응답에서 특정 방 통계 찾기
    private RoomStatisticsDTO findStats(RoomDashboardResponse response, Long roomId) {
        return response.roomList().stream()
                .filter(r -> r.roomId().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("통계 응답에 roomId " + roomId + "가 없습니다."));
    }

    // [신규 헬퍼] 상세 조회 응답에서 특정 카메라 찾기 - 반환 타입 수정
    private CameraEdgeWithIsFireDTO findCamera(RoomDetailResponse response, Long cameraId) {
        return response.cameras().stream()
                .filter(c -> c.cameraId().equals(cameraId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("상세 응답에 cameraId " + cameraId + "가 없습니다."));
    }
}