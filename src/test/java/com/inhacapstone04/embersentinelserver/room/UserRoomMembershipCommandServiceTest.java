package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.dto.RoomMemberDTO;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Room Membership Command Service의 비즈니스 로직 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserRoomMembershipCommandServiceTest {

    @InjectMocks
    private UserRoomMembershipCommandService userMembershipCommandService;

    // 의존성 Mocking
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoomMembershipRepository membershipRepository;

    // 테스트 상수
    private final Long ROOM_ID = 10L;
    private final Long REQUESTER_EDITOR_ID = 1L;
    private final Long REQUESTER_VIEWER_ID = 2L;
    private final Long TARGET_USER_ID = 3L;
    private final String TARGET_USER_EMAIL = "newuser@test.com";

    // Mock 엔티티 헬퍼
    private User createMockUser(Long id, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        when(user.getNickname()).thenReturn("TestNickname");
        when(user.getProfileImageUrl()).thenReturn("http://profile.img");
        when(user.getAuthType()).thenReturn(AuthType.GOOGLE);
        return user;
    }

    private UserRoomMembership createMockMembership(User user, MembershipRole role) {
        UserRoomMembership membership = mock(UserRoomMembership.class);
        when(membership.getUser()).thenReturn(user);
        when(membership.getRole()).thenReturn(role);
        return membership;
    }

    @Test
    @DisplayName("성공: 모든 검증 통과 후 멤버십 추가 성공")
    void addMemberToRoom_Success() {
        // Given
        User requester = createMockUser(REQUESTER_EDITOR_ID, "editor@test.com");
        User targetUser = createMockUser(TARGET_USER_ID, TARGET_USER_EMAIL);
        Room room = mock(Room.class);
        RoomMemberAddRequest request = new RoomMemberAddRequest(TARGET_USER_EMAIL, MembershipRole.VIEWER);

        // 1. 권한 검증 설정 (EDITOR 권한 보유)
        UserRoomMembership editorMembership = createMockMembership(requester, MembershipRole.EDITOR);
        when(membershipRepository.findByUser_IdAndRoom_Id(REQUESTER_EDITOR_ID, ROOM_ID))
                .thenReturn(Optional.of(editorMembership));

        // 2. 대상 사용자 존재 확인 설정
        when(userRepository.findByEmail(TARGET_USER_EMAIL)).thenReturn(Optional.of(targetUser));

        // 3. 중복 검증 설정 (아직 멤버 아님)
        when(membershipRepository.existsByUser_IdAndRoom_Id(TARGET_USER_ID, ROOM_ID)).thenReturn(false);

        // 4. Room 엔티티 조회 설정
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

        // ArgumentCaptor: 저장될 멤버십 엔티티를 캡처하여 검증
        ArgumentCaptor<UserRoomMembership> membershipCaptor = ArgumentCaptor.forClass(UserRoomMembership.class);

        // When
        RoomMemberDTO result = userMembershipCommandService.addMemberToRoom(
                REQUESTER_EDITOR_ID, ROOM_ID, request
        );

        // Then
        // 1. 멤버십 저장 호출 검증
        verify(membershipRepository, times(1)).save(membershipCaptor.capture());

        // 2. 저장된 엔티티의 역할 검증
        assertThat(membershipCaptor.getValue().getRole()).isEqualTo(MembershipRole.VIEWER);

        // 3. 반환 DTO 검증
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TARGET_USER_ID);
        assertThat(result.role()).isEqualTo(MembershipRole.VIEWER.name());
    }

    @Test
    @DisplayName("실패: 요청자가 Room의 멤버가 아닐 경우 (403 FORBIDDEN)")
    void addMemberToRoom_Fail_RequesterNotMember() {
        // Given
        RoomMemberAddRequest request = new RoomMemberAddRequest(TARGET_USER_EMAIL, MembershipRole.EDITOR);

        // 1. 권한 검증 설정 (멤버십 없음)
        when(membershipRepository.findByUser_IdAndRoom_Id(REQUESTER_EDITOR_ID, ROOM_ID))
                .thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                userMembershipCommandService.addMemberToRoom(REQUESTER_EDITOR_ID, ROOM_ID, request)
        );

        // 예외 코드 검증
        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);

        // 추가 로직 호출 안 됨 확인
        verify(userRepository, never()).findByEmail(anyString());
        verify(membershipRepository, never()).existsByUser_IdAndRoom_Id(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 요청자의 권한이 멤버 추가에 필요한 권한(EDITOR)보다 낮을 경우 (403 FORBIDDEN)")
    void addMemberToRoom_Fail_InsufficientPermission() {
        // Given
        User requester = createMockUser(REQUESTER_VIEWER_ID, "viewer@test.com");
        RoomMemberAddRequest request = new RoomMemberAddRequest(TARGET_USER_EMAIL, MembershipRole.EDITOR);

        // 1. 권한 검증 설정 (VIEWER 권한 보유)
        UserRoomMembership viewerMembership = createMockMembership(requester, MembershipRole.VIEWER);
        when(membershipRepository.findByUser_IdAndRoom_Id(REQUESTER_VIEWER_ID, ROOM_ID))
                .thenReturn(Optional.of(viewerMembership));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                userMembershipCommandService.addMemberToRoom(REQUESTER_VIEWER_ID, ROOM_ID, request)
        );

        // 예외 코드 검증 (EDITOR 미만은 권한 부족)
        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID);

        // 추가 로직 호출 안 됨 확인
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("실패: 추가하려는 대상 사용자가 서비스에 존재하지 않을 경우 (404 NOT_FOUND)")
    void addMemberToRoom_Fail_TargetUserNotFound() {
        // Given
        User requester = createMockUser(REQUESTER_EDITOR_ID, "editor@test.com");
        RoomMemberAddRequest request = new RoomMemberAddRequest(TARGET_USER_EMAIL, MembershipRole.VIEWER);

        // 1. 권한 검증 설정
        UserRoomMembership editorMembership = createMockMembership(requester, MembershipRole.EDITOR);
        when(membershipRepository.findByUser_IdAndRoom_Id(REQUESTER_EDITOR_ID, ROOM_ID))
                .thenReturn(Optional.of(editorMembership));

        // 2. 대상 사용자 존재 확인 설정 (없음)
        when(userRepository.findByEmail(TARGET_USER_EMAIL)).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                userMembershipCommandService.addMemberToRoom(REQUESTER_EDITOR_ID, ROOM_ID, request)
        );

        // 예외 코드 검증
        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND_BY_EMAIL);

        // 중복 검증 로직 호출 안 됨 확인
        verify(membershipRepository, never()).existsByUser_IdAndRoom_Id(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 대상 사용자가 이미 해당 Room의 멤버일 경우 (409 CONFLICT)")
    void addMemberToRoom_Fail_AlreadyMember() {
        // Given
        User requester = createMockUser(REQUESTER_EDITOR_ID, "editor@test.com");
        RoomMemberAddRequest request = new RoomMemberAddRequest(TARGET_USER_EMAIL, MembershipRole.VIEWER);
        User targetUser = createMockUser(TARGET_USER_ID, TARGET_USER_EMAIL);

        // 1. 권한 검증 설정
        UserRoomMembership editorMembership = createMockMembership(requester, MembershipRole.EDITOR);
        when(membershipRepository.findByUser_IdAndRoom_Id(REQUESTER_EDITOR_ID, ROOM_ID))
                .thenReturn(Optional.of(editorMembership));

        // 2. 대상 사용자 존재 확인 설정
        when(userRepository.findByEmail(TARGET_USER_EMAIL)).thenReturn(Optional.of(targetUser));

        // 3. 중복 검증 설정 (이미 멤버임)
        when(membershipRepository.existsByUser_IdAndRoom_Id(TARGET_USER_ID, ROOM_ID)).thenReturn(true);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                userMembershipCommandService.addMemberToRoom(REQUESTER_EDITOR_ID, ROOM_ID, request)
        );

        // 예외 코드 검증
        assertThat(exception.getCode()).isEqualTo(ErrorCode.ALREADY_MEMBER_OF_ROOM);

        // 멤버십 생성/저장 로직 호출 안 됨 확인
        verify(roomRepository, never()).findById(anyLong());
        verify(membershipRepository, never()).save(any(UserRoomMembership.class));
    }
}