package com.inhacapstone04.embersentinelserver.camera_edge;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.camera_edge.dto.request.CameraEdgeCreateRequest;
import com.inhacapstone04.embersentinelserver.camera_edge.dto.response.CameraEdgeResponse;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.camera_edge.service.CameraEdgeCommandService;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.UserRole;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional // 테스트 후 롤백 보장
@DisplayName("CameraEdgeCommandService 통합 테스트")
class CameraEdgeCommandServiceTest {

    @Autowired
    private CameraEdgeCommandService cameraEdgeCommandService;

    // 실제 Repository 주입
    @Autowired private CameraEdgeRepository cameraEdgeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoomMembershipRepository membershipRepository;
    @Autowired private BuildingRepository buildingRepository;

    // 테스트 데이터
    private User editorUser;
    private User viewerUser;
    private Room roomA;
    private Building building;

    private final String DEVICE_UUID = "test-uuid-1234";
    private final String CAMERA_ALIAS = "Test Camera";

    @BeforeEach
    void setUp() {
        // 1. 유저 생성
        editorUser = createUser("editor@test.com", "Editor");
        viewerUser = createUser("viewer@test.com", "Viewer");

        // 2. 빌딩 및 방 생성 (Building 필수)
        building = createBuilding("Test Building");
        roomA = createRoom("Room A", building);

        // 3. 멤버십 설정
        createMembership(editorUser, roomA, MembershipRole.EDITOR);
        createMembership(viewerUser, roomA, MembershipRole.VIEWER);
    }

    // --- Create Camera Edge Tests ---

    @Test
    @DisplayName("성공: EDITOR 권한으로 카메라 생성 성공")
    void createCameraEdge_Success() {
        // Given
        CameraEdgeCreateRequest request = new CameraEdgeCreateRequest(DEVICE_UUID, CAMERA_ALIAS);

        // When
        CameraEdgeResponse result = cameraEdgeCommandService.createCameraEdge(editorUser.getId(), roomA.getId(), request);

        // Then
        // 1. 반환값 검증
        assertThat(result).isNotNull();
        assertThat(result.deviceUuid()).isEqualTo(DEVICE_UUID);
        assertThat(result.cameraEdgeAlias()).isEqualTo(CAMERA_ALIAS);

        // 2. DB 저장 확인
        Optional<CameraEdge> savedCamera = cameraEdgeRepository.findById(result.cameraId());
        assertThat(savedCamera).isPresent();
        assertThat(savedCamera.get().getRoom().getId()).isEqualTo(roomA.getId());
    }

    @Test
    @DisplayName("실패: VIEWER 권한으로 카메라 생성 시도 (403 Forbidden)")
    void createCameraEdge_Fail_PermissionDenied() {
        // Given
        CameraEdgeCreateRequest request = new CameraEdgeCreateRequest(DEVICE_UUID, CAMERA_ALIAS);

        // When & Then
        assertThatThrownBy(() ->
                cameraEdgeCommandService.createCameraEdge(viewerUser.getId(), roomA.getId(), request)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);

        // DB에 저장되지 않았는지 확인
        assertThat(cameraEdgeRepository.existsByDeviceUuid(DEVICE_UUID)).isFalse();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Room ID (404 Not Found)")
    void createCameraEdge_Fail_RoomNotFound() {
        // Given
        Long nonExistentRoomId = 9999L;
        // (주의: 권한 검증이 먼저 실행되므로, 존재하지 않는 Room에 대한 권한 검증 시 403이 뜰 수도 있고,
        // 로직 순서에 따라 404가 뜰 수도 있음. 현재 로직상 권한 검증(UserMembershipCommandService)이 먼저 실행됨.
        // UserMembershipCommandService는 멤버십이 없으면 403을 던짐.
        // 따라서, "내 방이 아닌 방"에 카메라를 추가하려는 시도로 간주되어 403 Forbidden이 더 적절한 시나리오일 수 있음.

        // *현재 로직*: validateRequesterPermission -> findById
        // 존재하지 않는 방에는 멤버십도 없으므로 validateRequesterPermission에서 NOT_AUTHORIZED_ACCESS_BY_ID 발생 예상

        CameraEdgeCreateRequest request = new CameraEdgeCreateRequest(DEVICE_UUID, CAMERA_ALIAS);

        // When & Then
        assertThatThrownBy(() ->
                cameraEdgeCommandService.createCameraEdge(editorUser.getId(), nonExistentRoomId, request)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    @Test
    @DisplayName("실패: 이미 등록된 Device UUID 중복 (409 Conflict)")
    void createCameraEdge_Fail_DuplicateUuid() {
        // Given
        // 먼저 하나 생성
        CameraEdgeCreateRequest request1 = new CameraEdgeCreateRequest(DEVICE_UUID, "Cam 1");
        cameraEdgeCommandService.createCameraEdge(editorUser.getId(), roomA.getId(), request1);

        // 동일 UUID로 다시 생성 시도
        CameraEdgeCreateRequest request2 = new CameraEdgeCreateRequest(DEVICE_UUID, "Cam 2");

        // When & Then
        assertThatThrownBy(() ->
                cameraEdgeCommandService.createCameraEdge(editorUser.getId(), roomA.getId(), request2)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.DUPLICATE_DEVICE_UUID);
    }

    // --- Delete Camera Edge Tests ---

    @Test
    @DisplayName("성공: EDITOR 권한으로 카메라 삭제 성공")
    void deleteCameraEdge_Success() {
        // Given
        // 카메라 미리 생성
        CameraEdge camera = createCamera(roomA, DEVICE_UUID, CAMERA_ALIAS);
        Long cameraId = camera.getId();

        // When
        cameraEdgeCommandService.deleteCameraEdge(editorUser.getId(), roomA.getId(), cameraId);

        // Then
        assertThat(cameraEdgeRepository.findById(cameraId)).isEmpty();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 카메라 삭제 시도 (404 Not Found)")
    void deleteCameraEdge_Fail_CameraNotFound() {
        // Given
        Long nonExistentCameraId = 9999L;

        // When & Then
        assertThatThrownBy(() ->
                cameraEdgeCommandService.deleteCameraEdge(editorUser.getId(), roomA.getId(), nonExistentCameraId)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }

    @Test
    @DisplayName("실패: 다른 방에 속한 카메라 삭제 시도 (404 Not Found)")
    void deleteCameraEdge_Fail_CameraInDifferentRoom() {
        // Given
        // 다른 방 생성
        Room roomB = createRoom("Room B", building);
        createMembership(editorUser, roomB, MembershipRole.EDITOR); // editorUser가 roomB 권한도 있다고 가정

        // Room A에 카메라 생성
        CameraEdge cameraInRoomA = createCamera(roomA, DEVICE_UUID, CAMERA_ALIAS);

        // When & Then
        // Room B의 컨텍스트에서 Room A의 카메라를 삭제 시도 -> 소속 불일치로 404
        assertThatThrownBy(() ->
                cameraEdgeCommandService.deleteCameraEdge(editorUser.getId(), roomB.getId(), cameraInRoomA.getId())
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);

        // 삭제되지 않아야 함
        assertThat(cameraEdgeRepository.findById(cameraInRoomA.getId())).isPresent();
    }

    // --- 헬퍼 메서드 ---

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
        if (membershipRepository.existsByUser_IdAndRoom_Id(user.getId(), room.getId())) return;
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
}
