package com.inhacapstone04.embersentinelserver.auth;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.RedisService;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import com.inhacapstone04.embersentinelserver.user.config.OAuth2ClientProvider;
import com.inhacapstone04.embersentinelserver.user.dto.UserLoginResultDTO;
import com.inhacapstone04.embersentinelserver.user.dto.request.EmailLoginRequest;
import com.inhacapstone04.embersentinelserver.user.dto.response.AuthInfoResponse;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.OAuth2UserInfo;
import com.inhacapstone04.embersentinelserver.user.service.AuthService;
import com.inhacapstone04.embersentinelserver.user.service.UserCommandService;
import com.inhacapstone04.embersentinelserver.user.service.oauth.OAuth2ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserCommandService userCommandService;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisService redisService;
    @Mock private OAuth2ClientProvider oAuth2ClientProvider;

    private final Long TEST_USER_ID = 1L;
    private final String VALID_REFRESH_TOKEN = "validRefreshToken";
    private final String NEW_ACCESS_TOKEN = "newAccessToken";
    private final String NEW_REFRESH_TOKEN = "newRefreshToken";
    private final long ACCESS_EXP_MS = 3600000;
    private final long REFRESH_EXP_MS = 604800000;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", ACCESS_EXP_MS);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationTime", REFRESH_EXP_MS);
    }

    // --- reissueToken 테스트 ---

    @Test
    @DisplayName("성공: 유효한 RefreshToken으로 토큰 재발급 + 블랙리스트 등록")
    void reissueToken_Success() {
        // Given
        doNothing().when(jwtUtil).validateRefreshToken(VALID_REFRESH_TOKEN);
        when(redisService.hasKey(anyString())).thenReturn(false); // BL 키 없음
        when(jwtUtil.getUserIdFromToken(VALID_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);
        when(redisService.getValues("RT:" + TEST_USER_ID)).thenReturn(VALID_REFRESH_TOKEN);
        when(jwtUtil.generateAccessToken(TEST_USER_ID)).thenReturn(NEW_ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(TEST_USER_ID)).thenReturn(NEW_REFRESH_TOKEN);

        // When
        AuthInfoResponse response = authService.reissueToken(VALID_REFRESH_TOKEN);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(response.accessTokenExpiresIn()).isEqualTo(ACCESS_EXP_MS / 1000);
        assertThat(response.isNewUser()).isFalse();

        verify(jwtUtil).validateRefreshToken(VALID_REFRESH_TOKEN);
        verify(redisService).getValues("RT:" + TEST_USER_ID);
        // 새 RT 저장 + 이전 토큰 BL 등록 = setValues 2회 호출
        verify(redisService, times(2)).setValues(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("실패: Redis 저장 토큰과 불일치 (탈취 의심)")
    void reissueToken_Failure_TokenMismatch() {
        // Given
        doNothing().when(jwtUtil).validateRefreshToken(VALID_REFRESH_TOKEN);
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(jwtUtil.getUserIdFromToken(VALID_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);
        when(redisService.getValues("RT:" + TEST_USER_ID)).thenReturn("differentToken");

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(VALID_REFRESH_TOKEN));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
        verify(jwtUtil, never()).generateAccessToken(anyLong());
    }

    @Test
    @DisplayName("실패: RefreshToken 만료")
    void reissueToken_Failure_TokenExpired() {
        // Given
        doThrow(new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED))
                .when(jwtUtil).validateRefreshToken(VALID_REFRESH_TOKEN);

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(VALID_REFRESH_TOKEN));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED);
        verify(redisService, never()).getValues(anyString());
    }

    @Test
    @DisplayName("실패: 블랙리스트에 등록된 토큰 재사용 감지 → 모든 토큰 무효화")
    void reissueToken_Fail_BlacklistedToken() {
        // Given
        doNothing().when(jwtUtil).validateRefreshToken(VALID_REFRESH_TOKEN);
        when(redisService.hasKey(anyString())).thenReturn(true); // BL 키 존재
        when(jwtUtil.getUserIdFromToken(VALID_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(VALID_REFRESH_TOKEN));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);
        // 해당 사용자의 RT 삭제 검증
        verify(redisService).deleteValues("RT:" + TEST_USER_ID);
    }

    // --- login 테스트 ---

    @Test
    @DisplayName("성공: OAuth 로그인 → findOrCreateUser → JWT+RT 발급")
    void login_Success() {
        // Given
        String thirdPartyToken = "google-access-token";
        OAuth2ClientService mockClient = mock(OAuth2ClientService.class);
        OAuth2UserInfo mockUserInfo = mock(OAuth2UserInfo.class);
        User mockUser = mock(User.class);

        when(oAuth2ClientProvider.getClient(AuthType.GOOGLE)).thenReturn(mockClient);
        when(mockClient.getUserInfo(thirdPartyToken)).thenReturn(mockUserInfo);
        when(userCommandService.findOrCreateUser(mockUserInfo))
                .thenReturn(UserLoginResultDTO.of(mockUser, true));
        when(mockUser.getId()).thenReturn(TEST_USER_ID);
        when(jwtUtil.generateAccessToken(TEST_USER_ID)).thenReturn(NEW_ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(TEST_USER_ID)).thenReturn(NEW_REFRESH_TOKEN);

        // When
        AuthInfoResponse response = authService.login(AuthType.GOOGLE, thirdPartyToken);

        // Then
        assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(response.isNewUser()).isTrue();

        // RT Redis 저장 검증
        verify(redisService).setValues(eq("RT:" + TEST_USER_ID), eq(NEW_REFRESH_TOKEN), any(Duration.class));
    }

    // --- loginByEmail 테스트 ---

    @Test
    @DisplayName("성공: 이메일 로그인 → findOrCreateUserByEmail → JWT+RT 발급")
    void loginByEmail_Success() {
        // Given
        EmailLoginRequest request = new EmailLoginRequest("test@test.com", "TestUser");
        User mockUser = mock(User.class);

        when(userCommandService.findOrCreateUserByEmail(request))
                .thenReturn(UserLoginResultDTO.of(mockUser, false));
        when(mockUser.getId()).thenReturn(TEST_USER_ID);
        when(jwtUtil.generateAccessToken(TEST_USER_ID)).thenReturn(NEW_ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(TEST_USER_ID)).thenReturn(NEW_REFRESH_TOKEN);

        // When
        AuthInfoResponse response = authService.loginByEmail(AuthType.EMAIL, request);

        // Then
        assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(response.isNewUser()).isFalse();

        verify(redisService).setValues(eq("RT:" + TEST_USER_ID), eq(NEW_REFRESH_TOKEN), any(Duration.class));
    }
}
