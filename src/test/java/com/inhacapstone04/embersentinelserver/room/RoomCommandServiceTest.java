package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.dto.request.RoomCreateRequest;
import com.inhacapstone04.embersentinelserver.room.dto.response.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import com.inhacapstone04.embersentinelserver.room.service.RoomCommandService;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomCommandServiceTest {

    @InjectMocks
    private RoomCommandService roomCommandService;

    @Mock private RoomRepository roomRepository;
    @Mock private UserRoomMembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private BuildingRepository buildingRepository;

    private final Long USER_ID = 1L;
    private final Long BUILDING_ID = 10L;
    private final Long ROOM_ID = 100L;

    // --- createRoom ---

    @Test
    @DisplayName("성공: Room 생성 + EDITOR 멤버십 자동 생성")
    void createRoom_Success() {
        // Given
        RoomCreateRequest request = new RoomCreateRequest(BUILDING_ID, "연구실", "3", "305");

        User user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);

        Building building = new Building();
        ReflectionTestUtils.setField(building, "id", BUILDING_ID);
        building.setBuildingName("하이테크센터");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(buildingRepository.findById(BUILDING_ID)).thenReturn(Optional.of(building));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            return room;
        });

        // When
        SingleRoomResponse response = roomCommandService.createRoom(USER_ID, request);

        // Then
        assertThat(response.roomId()).isEqualTo(ROOM_ID);
        assertThat(response.roomAlias()).isEqualTo("연구실");
        assertThat(response.buildingName()).isEqualTo("하이테크센터");

        verify(roomRepository).save(any(Room.class));
        verify(membershipRepository).save(any(UserRoomMembership.class));
    }

    @Test
    @DisplayName("실패: 사용자 미존재 → USER_NOT_FOUND")
    void createRoom_Fail_UserNotFound() {
        // Given
        RoomCreateRequest request = new RoomCreateRequest(BUILDING_ID, "연구실", "3", "305");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> roomCommandService.createRoom(USER_ID, request));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 빌딩 미존재 → NOT_FOUND_BY_ID")
    void createRoom_Fail_BuildingNotFound() {
        // Given
        RoomCreateRequest request = new RoomCreateRequest(BUILDING_ID, "연구실", "3", "305");
        User user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(buildingRepository.findById(BUILDING_ID)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> roomCommandService.createRoom(USER_ID, request));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND_BY_ID);
    }

    // --- deleteRoom ---

    @Test
    @DisplayName("성공: EDITOR 권한으로 삭제")
    void deleteRoom_Success_Editor() {
        // Given
        UserRoomMembership membership = mock(UserRoomMembership.class);
        when(membership.getRole()).thenReturn(MembershipRole.EDITOR);

        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", ROOM_ID);

        when(membershipRepository.findByUser_IdAndRoom_Id(USER_ID, ROOM_ID))
                .thenReturn(Optional.of(membership));
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

        // When
        roomCommandService.deleteRoom(USER_ID, ROOM_ID);

        // Then
        verify(roomRepository).delete(room);
    }

    @Test
    @DisplayName("실패: VIEWER 권한 부족 → NOT_AUTHORIZED_ACCESS_BY_ID")
    void deleteRoom_Fail_ViewerRole() {
        // Given
        UserRoomMembership membership = mock(UserRoomMembership.class);
        when(membership.getRole()).thenReturn(MembershipRole.VIEWER);

        when(membershipRepository.findByUser_IdAndRoom_Id(USER_ID, ROOM_ID))
                .thenReturn(Optional.of(membership));

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> roomCommandService.deleteRoom(USER_ID, ROOM_ID));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    @Test
    @DisplayName("실패: 멤버십 없음 → NOT_AUTHORIZED_ACCESS_BY_ID")
    void deleteRoom_Fail_NoMembership() {
        // Given
        when(membershipRepository.findByUser_IdAndRoom_Id(USER_ID, ROOM_ID))
                .thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> roomCommandService.deleteRoom(USER_ID, ROOM_ID));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);
    }

    @Test
    @DisplayName("실패: Room 미존재 → NOT_FOUND_BY_ID")
    void deleteRoom_Fail_RoomNotFound() {
        // Given
        UserRoomMembership membership = mock(UserRoomMembership.class);
        when(membership.getRole()).thenReturn(MembershipRole.EDITOR);

        when(membershipRepository.findByUser_IdAndRoom_Id(USER_ID, ROOM_ID))
                .thenReturn(Optional.of(membership));
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> roomCommandService.deleteRoom(USER_ID, ROOM_ID));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND_BY_ID);
    }
}
