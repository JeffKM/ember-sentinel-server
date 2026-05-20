package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.dto.request.RoomCreateRequest;
import com.inhacapstone04.embersentinelserver.room.dto.response.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.service.RoomCommandService;
import com.inhacapstone04.embersentinelserver.support.IntegrationTestSupport;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("RoomCommandService 통합 테스트")
class RoomCommandServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RoomCommandService roomCommandService;

    private User testUser;
    private Building testBuilding;

    @BeforeEach
    void setUp() {
        testUser = createUser("test@test.com", "TestUser");
        testBuilding = createBuilding("하이테크센터");
    }

    @Test
    @DisplayName("성공: Room 생성 → DB에 Room + Membership 저장")
    void createRoom_Success() {
        // Given
        RoomCreateRequest request = new RoomCreateRequest(
                testBuilding.getId(), "정보보안연구실", "3", "305"
        );

        // When
        SingleRoomResponse response = roomCommandService.createRoom(testUser.getId(), request);

        // Then
        assertThat(response.roomId()).isNotNull();
        assertThat(response.roomAlias()).isEqualTo("정보보안연구실");
        assertThat(response.buildingName()).isEqualTo("하이테크센터");
        assertThat(response.floor()).isEqualTo("3");
        assertThat(response.roomNumber()).isEqualTo("305");

        // EDITOR 멤버십 자동 생성 검증
        assertThat(membershipRepository.existsByUser_IdAndRoom_Id(testUser.getId(), response.roomId()))
                .isTrue();
    }

    @Test
    @DisplayName("성공: Room 삭제 → cascade로 멤버십/카메라 함께 삭제")
    void deleteRoom_Success_Cascade() {
        // Given
        Room room = createRoom("삭제대상", "1", "101", testBuilding);
        createMembership(testUser, room, MembershipRole.EDITOR);
        CameraEdge camera = createCamera(room, "cam-uuid", "삭제 카메라");

        Long roomId = room.getId();
        Long cameraId = camera.getId();

        // When
        roomCommandService.deleteRoom(testUser.getId(), roomId);

        // Then
        assertThat(roomRepository.findById(roomId)).isEmpty();
        assertThat(cameraEdgeRepository.findById(cameraId)).isEmpty();
        assertThat(membershipRepository.existsByUser_IdAndRoom_Id(testUser.getId(), roomId)).isFalse();
    }

    @Test
    @DisplayName("실패: 비멤버가 삭제 시도 → NOT_AUTHORIZED_ACCESS_BY_ID")
    void deleteRoom_Fail_NotMember() {
        // Given
        User otherUser = createUser("other@test.com", "Other");
        Room room = createRoom("비멤버 삭제", "2", "202", testBuilding);
        createMembership(testUser, room, MembershipRole.EDITOR);

        // When & Then
        assertThatThrownBy(() -> roomCommandService.deleteRoom(otherUser.getId(), room.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }
}
