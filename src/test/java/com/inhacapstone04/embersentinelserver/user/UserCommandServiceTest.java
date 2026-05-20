package com.inhacapstone04.embersentinelserver.user;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.FcmService;
import com.inhacapstone04.embersentinelserver.user.dto.UserLoginResultDTO;
import com.inhacapstone04.embersentinelserver.user.dto.request.EmailLoginRequest;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.UserRole;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.OAuth2UserInfo;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import com.inhacapstone04.embersentinelserver.user.service.UserCommandService;
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
class UserCommandServiceTest {

    @InjectMocks
    private UserCommandService userCommandService;

    @Mock private UserRepository userRepository;
    @Mock private FcmService fcmService;

    private final Long USER_ID = 1L;

    @Test
    @DisplayName("성공: 기존 유저 → 정보 업데이트 + isNewUser=false")
    void findOrCreateUser_ExistingUser() {
        // Given
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(userInfo.getAuthType()).thenReturn(AuthType.GOOGLE);
        when(userInfo.getNickname()).thenReturn("Updated Name");
        when(userInfo.getProfileImageUrl()).thenReturn("https://img.url");

        User existingUser = new User();
        ReflectionTestUtils.setField(existingUser, "id", USER_ID);
        existingUser.setEmail("test@test.com");
        existingUser.setNickname("Old Name");

        when(userRepository.findByEmailAndAuthType("test@test.com", AuthType.GOOGLE))
                .thenReturn(Optional.of(existingUser));

        // When
        UserLoginResultDTO result = userCommandService.findOrCreateUser(userInfo);

        // Then
        assertThat(result.isNewUser()).isFalse();
        assertThat(result.user().getNickname()).isEqualTo("Updated Name");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공: 신규 유저 → 생성 + isNewUser=true")
    void findOrCreateUser_NewUser() {
        // Given
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.getEmail()).thenReturn("new@test.com");
        when(userInfo.getAuthType()).thenReturn(AuthType.GOOGLE);
        when(userInfo.getNickname()).thenReturn("New User");
        when(userInfo.getProfileImageUrl()).thenReturn("https://img.url");

        when(userRepository.findByEmailAndAuthType("new@test.com", AuthType.GOOGLE))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            ReflectionTestUtils.setField(u, "id", USER_ID);
            return u;
        });

        // When
        UserLoginResultDTO result = userCommandService.findOrCreateUser(userInfo);

        // Then
        assertThat(result.isNewUser()).isTrue();
        assertThat(result.user().getEmail()).isEqualTo("new@test.com");
        assertThat(result.user().getUserRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("성공: FCM 토큰 업데이트")
    void updateFcmToken_Success() {
        // Given
        User user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setFcmToken("old-token");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        userCommandService.updateFcmToken(USER_ID, "new-token");

        // Then
        assertThat(user.getFcmToken()).isEqualTo("new-token");
    }

    @Test
    @DisplayName("실패: FCM 토큰 업데이트 시 사용자 미존재 → NOT_FOUND_BY_ID")
    void updateFcmToken_Fail_UserNotFound() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> userCommandService.updateFcmToken(USER_ID, "token"));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND_BY_ID);
    }

    @Test
    @DisplayName("성공: 이메일 기반 사용자 조회/생성")
    void findOrCreateUserByEmail_Success() {
        // Given
        EmailLoginRequest request = new EmailLoginRequest("email@test.com", "TestUser");

        when(userRepository.findByEmailAndAuthType("email@test.com", AuthType.EMAIL))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            ReflectionTestUtils.setField(u, "id", USER_ID);
            return u;
        });

        // When
        UserLoginResultDTO result = userCommandService.findOrCreateUserByEmail(request);

        // Then
        assertThat(result.isNewUser()).isTrue();
        assertThat(result.user().getAuthType()).isEqualTo(AuthType.EMAIL);
    }
}
