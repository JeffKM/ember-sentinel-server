package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.dto.RoomMemberResponse;
import com.inhacapstone04.embersentinelserver.room.dto.request.RoomMemberAddRequest;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import com.inhacapstone04.embersentinelserver.room.service.UserRoomMembershipCommandService;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserRoomMembershipCommandService 통합 테스트
 * 실제 DB (H2/Test DB)와 Repository를 사용하여 비즈니스 로직을 검증합니다.
 */
@SpringBootTest
@Transactional // 테스트 후 DB 롤백을 위해 사용
@DisplayName("UserRoomMembershipCommandService 통합 테스트")
class UserRoomMembershipCommandServiceTest {

    @Autowired
    private UserRoomMembershipCommandService userRoomMembershipCommandService;

    // 실제 Repository 주입
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRoomMembershipRepository membershipRepository;
    // BuildingRepository 주입 추가
    @Autowired private BuildingRepository buildingRepository;

    // 테스트 상수 및 데이터
    private User requesterEditor;
    private User requesterViewer;
    private User targetUser;
    private Room roomA;
    private Building testBuilding; // Building 객체 추가

    @BeforeEach
    void setUp() {
        // 1. 요청자 유저 생성
        requesterEditor = createUser("editor@inha.ac.kr", "RequesterEditor");
        requesterViewer = createUser("viewer@inha.ac.kr", "RequesterViewer");

        // 2. 추가 대상 유저 생성
        targetUser = createUser("target@inha.ac.kr", "TargetUser");

        // 3. [추가됨] Building 생성
        testBuilding = createBuilding("테스트 빌딩");

        // 4. Room 생성 (Building 객체 전달)
        roomA = createRoom("Test Room A", testBuilding);

        // 5. 요청자 멤버십 설정
        // Editor 권한 요청자 (멤버 추가 권한 O)
        createMembership(requesterEditor, roomA, MembershipRole.EDITOR);
        // Viewer 권한 요청자 (멤버 추가 권한 X)
        createMembership(requesterViewer, roomA, MembershipRole.VIEWER);

        // targetUser는 초기 멤버가 아님
    }

    // --- ADD MEMBER TESTS ---

    @Test
    @DisplayName("성공: EDITOR가 새로운 멤버를 VIEWER 역할로 추가")
    void addMemberToRoom_Success() {
        // Given
        Long requestingUserId = requesterEditor.getId();
        Long roomId = roomA.getId();
        RoomMemberAddRequest request = new RoomMemberAddRequest(targetUser.getEmail(), MembershipRole.VIEWER);

        // When
        RoomMemberResponse result = userRoomMembershipCommandService.addMemberToRoom(requestingUserId, roomId, request);

        // Then
        // 1. 응답 DTO 검증
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(targetUser.getId());
        assertThat(result.role()).isEqualTo(MembershipRole.VIEWER.name());

        // 2. DB 저장 확인 (실제 멤버십이 생성되었는지 확인)
        assertThat(membershipRepository.existsByUser_IdAndRoom_Id(targetUser.getId(), roomId)).isTrue();

        // 3. 저장된 멤버십의 역할 검증
        UserRoomMembership newMembership = membershipRepository.findByUser_IdAndRoom_Id(targetUser.getId(), roomId).get();
        assertThat(newMembership.getRole()).isEqualTo(MembershipRole.VIEWER);
    }

    @Test
    @DisplayName("실패: 요청자가 Room의 멤버가 아닐 경우 (403 FORBIDDEN)")
    void addMemberToRoom_Fail_RequesterNotMember() {
        // Given
        // nonMemberId는 현재 DB에 존재하며, roomA의 멤버십만 없는 상태
        // 이 테스트는 nonMemberId를 사용하므로, 
        // targetUser는 다른 테스트에 이미 사용되어서 재활용이 어려우므로 새로운 유저를 생성합니다.
        User nonMemberUser = createUser("nonmember@test.com", "NonMemberUser");

        RoomMemberAddRequest request = new RoomMemberAddRequest(targetUser.getEmail(), MembershipRole.EDITOR);

        // When & Then
        assertThatThrownBy(() -> userRoomMembershipCommandService.addMemberToRoom(nonMemberUser.getId(), roomA.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);

        // DB에 멤버십이 추가되지 않았는지 확인
        assertThat(membershipRepository.existsByUser_IdAndRoom_Id(targetUser.getId(), roomA.getId())).isFalse();
    }

    @Test
    @DisplayName("실패: 요청자의 권한이 멤버 추가에 필요한 권한(EDITOR)보다 낮을 경우 (403 FORBIDDEN)")
    void addMemberToRoom_Fail_InsufficientPermission() {
        // Given
        Long viewerId = requesterViewer.getId();
        RoomMemberAddRequest request = new RoomMemberAddRequest(targetUser.getEmail(), MembershipRole.EDITOR);

        // When & Then
        assertThatThrownBy(() -> userRoomMembershipCommandService.addMemberToRoom(viewerId, roomA.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);

        // DB에 멤버십이 추가되지 않았는지 확인
        assertThat(membershipRepository.existsByUser_IdAndRoom_Id(targetUser.getId(), roomA.getId())).isFalse();
    }

    @Test
    @DisplayName("실패: 추가하려는 대상 사용자가 서비스에 존재하지 않을 경우 (404 NOT_FOUND_BY_EMAIL)")
    void addMemberToRoom_Fail_TargetUserNotFound() {
        // Given
        Long requestingUserId = requesterEditor.getId();
        RoomMemberAddRequest request = new RoomMemberAddRequest("nonexistent@user.com", MembershipRole.VIEWER);

        // When & Then
        assertThatThrownBy(() -> userRoomMembershipCommandService.addMemberToRoom(requestingUserId, roomA.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_EMAIL);
    }

    @Test
    @DisplayName("실패: 대상 사용자가 이미 해당 Room의 멤버일 경우 (409 CONFLICT)")
    void addMemberToRoom_Fail_AlreadyMember() {
        // Given
        Long requestingUserId = requesterEditor.getId();
        // 이미 Room의 멤버인 requesterViewer를 추가 시도
        RoomMemberAddRequest request = new RoomMemberAddRequest(requesterViewer.getEmail(), MembershipRole.VIEWER);

        // When & Then
        assertThatThrownBy(() -> userRoomMembershipCommandService.addMemberToRoom(requestingUserId, roomA.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ALREADY_MEMBER_OF_ROOM);
    }

    // --- REMOVE MEMBER TESTS ---

    @Test
    @DisplayName("성공: EDITOR가 다른 멤버를 Room에서 삭제 성공")
    void removeMemberFromRoom_Success() {
        // Given
        Long requestingUserId = requesterEditor.getId();
        Long memberToDeleteId = targetUser.getId();

        // 삭제 대상 멤버십 미리 생성
        createMembership(targetUser, roomA, MembershipRole.VIEWER);
        assertThat(membershipRepository.existsByUser_IdAndRoom_Id(memberToDeleteId, roomA.getId())).isTrue();

        // When
        userRoomMembershipCommandService.removeMemberFromRoom(requestingUserId, roomA.getId(), memberToDeleteId);

        // Then
        // 1. DB에서 멤버십이 삭제되었는지 확인
        assertThat(membershipRepository.existsByUser_IdAndRoom_Id(memberToDeleteId, roomA.getId())).isFalse();
    }

    @Test
    @DisplayName("실패: 요청자가 멤버 삭제 권한(EDITOR)이 없을 경우 (403 FORBIDDEN)")
    void removeMemberFromRoom_Fail_InsufficientPermission() {
        // Given
        Long requestingUserId = requesterViewer.getId(); // Viewer 권한
        Long memberToDeleteId = targetUser.getId();

        // 삭제 대상 멤버십 미리 생성
        createMembership(targetUser, roomA, MembershipRole.VIEWER);

        // When & Then
        assertThatThrownBy(() -> userRoomMembershipCommandService.removeMemberFromRoom(requestingUserId, roomA.getId(), memberToDeleteId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);

        // 멤버십이 삭제되지 않고 남아있는지 확인
        assertThat(membershipRepository.existsByUser_IdAndRoom_Id(memberToDeleteId, roomA.getId())).isTrue();
    }

    @Test
    @DisplayName("실패: 요청자가 자기 자신을 삭제하려고 시도할 경우 (400 BAD_REQUEST)")
    void removeMemberFromRoom_Fail_CannotRemoveSelf() {
        // Given
        Long selfId = requesterEditor.getId();

        // When & Then
        assertThatThrownBy(() -> userRoomMembershipCommandService.removeMemberFromRoom(selfId, roomA.getId(), selfId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.CANNOT_REMOVE_SELF);
    }

    @Test
    @DisplayName("실패: 삭제 대상이 해당 Room의 멤버가 아닐 경우 (404 NOT_FOUND)")
    void removeMemberFromRoom_Fail_TargetNotMember() {
        // Given
        Long requestingUserId = requesterEditor.getId();
        Long nonMemberId = targetUser.getId(); // targetUser는 현재 roomA의 멤버가 아님 (setUp에서 생성 후 멤버십 연결 안 함)

        // When & Then
        assertThatThrownBy(() -> userRoomMembershipCommandService.removeMemberFromRoom(requestingUserId, roomA.getId(), nonMemberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }


    // --- 헬퍼 메서드 (데이터 생성) ---

    private User createUser(String email, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setAuthType(AuthType.GOOGLE); // 임의 설정
        user.setUserRole(com.inhacapstone04.embersentinelserver.user.entity.UserRole.USER); // UserRole 명시적 설정
        return userRepository.save(user);
    }

    // [추가됨] Building 생성 헬퍼 메서드
    private Building createBuilding(String name) {
        Building building = new Building();
        building.setBuildingName(name);
        // 필요한 다른 Building 필드 설정 (예: address 등)
        return buildingRepository.save(building);
    }

    // [수정됨] Room 생성 헬퍼 메서드: Building을 파라미터로 받도록 수정
    private Room createRoom(String alias, Building building) {
        Room room = new Room();
        room.setRoomAlias(alias);
        room.setBuilding(building); // Building 연결
        // Room 엔티티에 not null 제약이 있는 다른 필드 (예: floor, roomNumber)가 있다면 여기에서 설정 필요
        return roomRepository.save(room);
    }

    private void createMembership(User user, Room room, MembershipRole role) {
        // 이미 존재하는 멤버십은 무시하고 새로 생성
        if (membershipRepository.existsByUser_IdAndRoom_Id(user.getId(), room.getId())) {
            return;
        }
        UserRoomMembership membership = new UserRoomMembership(user, room);
        membership.setRole(role);
        membershipRepository.save(membership);
    }
}