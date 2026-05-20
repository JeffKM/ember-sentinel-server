package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.dto.RoomMemberResponse;
import com.inhacapstone04.embersentinelserver.room.dto.request.RoomMemberAddRequest;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.room.service.UserRoomMembershipCommandService;
import com.inhacapstone04.embersentinelserver.support.IntegrationTestSupport;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("UserRoomMembershipCommandService 통합 테스트")
class UserRoomMembershipCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private UserRoomMembershipCommandService userRoomMembershipCommandService;

    @Autowired
    private EntityManager entityManager;

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
        entityManager.flush();
        entityManager.clear();

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


    // 로컬 헬퍼: floor/roomNumber 없이 Room 생성
    private Room createRoom(String alias, Building building) {
        return createRoom(alias, null, null, building);
    }

    // 로컬 헬퍼: 중복 멤버십 방지
    private void createMembershipSafe(User user, Room room, MembershipRole role) {
        if (membershipRepository.existsByUser_IdAndRoom_Id(user.getId(), room.getId())) {
            return;
        }
        createMembership(user, room, role);
    }
}