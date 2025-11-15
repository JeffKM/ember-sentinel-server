package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import com.inhacapstone04.embersentinelserver.room.dto.RoomDashboardResponse;
import com.inhacapstone04.embersentinelserver.room.dto.RoomStatisticsDTO;
import com.inhacapstone04.embersentinelserver.room.dto.SingleRoomResponse;
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

import java.util.Collections;
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
    // (이 Repository들이 모두 Bean으로 등록되어 있어야 합니다)
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private UserRoomMembershipRepository membershipRepository;
    @Autowired
    private CameraEdgeRepository cameraEdgeRepository;
    @Autowired
    private FireEventRepository fireEventRepository;
    @Autowired
    private MediaStreamRepository mediaStreamRepository;

    private User testUser1;
    private User testUser2;
    private Room roomA, roomB, roomC_OtherUser;
    private CameraEdge camA1, camA2, camB1;

    /**
     * 테스트 데이터 세팅:
     * - User1: roomA, roomB에 접근 가능
     * - User2: roomC에 접근 가능
     *
     * - roomA: 카메라 2대 (camA1, camA2), LIVE 이벤트 1개 (camA1)
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

        // 3. 방 생성
        roomA = createRoom("정보보안연구실", "3", "305", building);
        roomB = createRoom("지능형보안연구실", "11", "1105", building);
        roomC_OtherUser = createRoom("서버실", "5", "501", building);

        // 4. 멤버십 연결
        createMembership(testUser1, roomA);
        createMembership(testUser1, roomB);
        createMembership(testUser2, roomC_OtherUser);

        // 5. 카메라 생성
        camA1 = createCamera(roomA);
        camA2 = createCamera(roomA); // roomA (카메라 2대)
        camB1 = createCamera(roomB); // roomB (카메라 1대)

        // 6. 화재 이벤트 및 미디어 스트림 생성
        // roomA - camA1: LIVE 이벤트 1개
        FireEvent eventA_Live = createFireEvent(camA1);
        createMediaStream(eventA_Live, StreamingStatus.LIVE);

        // roomA - camA2: ENDED 이벤트 1개 (카운트되면 안 됨)
        FireEvent eventA_Ended = createFireEvent(camA2);
        createMediaStream(eventA_Ended, StreamingStatus.ENDED);

        // roomB - camB1: 이벤트 없음 (카운트 0)
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
        assertThat(response.pageNumber()).isEqualTo(1); // PageResponse는 1부터 시작
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(2); // roomA, roomB
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).roomAlias()).isEqualTo(roomA.getRoomAlias());
        assertThat(response.content().get(1).roomAlias()).isEqualTo(roomB.getRoomAlias());
    }

    @Test
    @DisplayName("내 방 목록 페이징 처리 (1페이지 1개씩)")
    void getMyRooms_Paging() {
        // given
        Pageable page1 = PageRequest.of(0, 1); // 0번 페이지 (1개)
        Pageable page2 = PageRequest.of(1, 1); // 1번 페이지 (1개)

        // when
        PageResponse<SingleRoomResponse> response1 = roomQueryService.getMyRooms(testUser1.getId(), page1);
        PageResponse<SingleRoomResponse> response2 = roomQueryService.getMyRooms(testUser1.getId(), page2);

        // then
        assertThat(response1.totalElements()).isEqualTo(2);
        assertThat(response1.totalPages()).isEqualTo(2);
        assertThat(response1.content()).hasSize(1); // 1개만 조회
        assertThat(response1.content().get(0).roomId()).isEqualTo(roomA.getId());

        assertThat(response2.content()).hasSize(1); // 1개만 조회
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
        assertThat(roomAStats.roomAlias()).isEqualTo(roomA.getRoomAlias());
        assertThat(roomAStats.cameraCountPerRoom()).isEqualTo(2);
        assertThat(roomAStats.fireEventCountPerRoom()).isEqualTo(1);

        // then (RoomB 상세)
        RoomStatisticsDTO roomBStats = findStats(response, roomB.getId());
        assertThat(roomBStats.roomAlias()).isEqualTo(roomB.getRoomAlias());
        assertThat(roomBStats.cameraCountPerRoom()).isEqualTo(1);
        assertThat(roomBStats.fireEventCountPerRoom()).isEqualTo(0);
    }

    @Test
    @DisplayName("대시보드 통계 조회 실패 (권한 없는 Room 포함 시 FORBIDDEN)")
    void getRoomStatistics_Fail_Forbidden() {
        // given
        // User1이 접근 불가능한 roomC_OtherUser의 ID를 포함하여 요청
        List<Long> requestedIds = List.of(roomA.getId(), roomC_OtherUser.getId());

        // when & then
        assertThatThrownBy(() -> roomQueryService.getRoomStatistics(testUser1.getId(), requestedIds))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID.getCode());
    }

    @Test
    @DisplayName("대시보드 통계 조회 (빈 리스트 요청 시)")
    void getRoomStatistics_EmptyList() {
        // given
        List<Long> requestedIds = Collections.emptyList();

        // when
        RoomDashboardResponse response = roomQueryService.getRoomStatistics(testUser1.getId(), requestedIds);

        // then
        assertThat(response.totalRoomCount()).isZero();
        assertThat(response.totalCameraCount()).isZero();
        assertThat(response.liveStreamCount()).isZero();
        assertThat(response.roomList()).isEmpty();
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
        building.setBuildingName(name); // (Building 엔티티에 buildingName 필드가 있다고 가정)
        return buildingRepository.save(building);
    }

    private Room createRoom(String alias, String floor, String roomNum, Building building) {
        Room room = new Room();
        room.setRoomAlias(alias);
        room.setBuildingLocationFloor(floor);
        room.setRoomNumber(roomNum);
        room.setBuilding(building);
        return roomRepository.save(room);
    }

    private void createMembership(User user, Room room) {
        UserRoomMembership membership = new UserRoomMembership(user, room);
        membership.setRole(MembershipRole.EDITOR);
        membershipRepository.save(membership);
    }

    private CameraEdge createCamera(Room room) {
        CameraEdge camera = new CameraEdge();
        camera.setRoom(room);
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

    private RoomStatisticsDTO findStats(RoomDashboardResponse response, Long roomId) {
        return response.roomList().stream()
                .filter(r -> r.roomId().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("통계 응답에 roomId " + roomId + "가 없습니다."));
    }
}
