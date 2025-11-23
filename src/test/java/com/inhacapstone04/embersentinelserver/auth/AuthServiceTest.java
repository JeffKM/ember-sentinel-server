package com.inhacapstone04.embersentinelserver.auth;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.RedisService;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import com.inhacapstone04.embersentinelserver.user.dto.response.AuthInfoResponse;
import com.inhacapstone04.embersentinelserver.user.service.AuthService;
import com.inhacapstone04.embersentinelserver.user.service.UserCommandService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AuthService의 reissueToken 메서드 테스트
 * RedisService와 JwtUtil을 Mocking하여 의존성을 분리합니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    // AuthService의 의존성을 Mocking
    @Mock
    private UserCommandService userCommandService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisService redisService;

    // 테스트에 필요한 상수 정의
    private final Long TEST_USER_ID = 1L;
    private final String VALID_REFRESH_TOKEN = "validRefreshToken";
    private final String NEW_ACCESS_TOKEN = "newAccessToken";
    private final String NEW_REFRESH_TOKEN = "newRefreshToken";
    private final long ACCESS_EXP_MS = 3600000; // 1시간
    private final long REFRESH_EXP_MS = 604800000; // 7일

    @BeforeEach
    void setUp() {
        // @Value로 주입되는 필드를 ReflectionTestUtils를 사용하여 수동으로 설정
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", ACCESS_EXP_MS);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationTime", REFRESH_EXP_MS);
    }

    @Test
    @DisplayName("성공: 유효한 RefreshToken으로 토큰 재발급 성공")
    void reissueToken_Success() {
        // Given
        // 1. Refresh Token이 유효함 (validateRefreshToken에서 예외 미발생)
        doNothing().when(jwtUtil).validateRefreshToken(VALID_REFRESH_TOKEN);

        // 2. Refresh Token에서 UserId 추출 성공
        when(jwtUtil.getUserIdFromToken(VALID_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);

        // 3. Redis에 저장된 토큰이 일치함
        when(redisService.getValues("RT:" + TEST_USER_ID)).thenReturn(VALID_REFRESH_TOKEN);

        // 4. 새로운 토큰 발급
        when(jwtUtil.generateAccessToken(TEST_USER_ID)).thenReturn(NEW_ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(TEST_USER_ID)).thenReturn(NEW_REFRESH_TOKEN);

        // 5. Redis 저장 로직은 성공한다고 가정
        doNothing().when(redisService).setValues(eq("RT:" + TEST_USER_ID), eq(NEW_REFRESH_TOKEN), any(Duration.class));


        // When
        AuthInfoResponse response = authService.reissueToken(VALID_REFRESH_TOKEN);

        // Then
        // 1. 응답 DTO 필드 검증
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(response.accessTokenExpiresIn()).isEqualTo(ACCESS_EXP_MS / 1000);
        assertThat(response.isNewUser()).isFalse();

        // 2. 핵심 로직 호출 검증
        // 토큰 유효성 검사, userId 추출이 호출되었는지 확인
        verify(jwtUtil, times(1)).validateRefreshToken(VALID_REFRESH_TOKEN);
        verify(jwtUtil, times(1)).getUserIdFromToken(VALID_REFRESH_TOKEN);

        // Redis 저장된 값과 비교하기 위해 조회 호출 검증
        verify(redisService, times(1)).getValues("RT:" + TEST_USER_ID);

        // 새로운 Refresh Token으로 Redis에 갱신 저장 호출 검증
        verify(redisService, times(1)).setValues(eq("RT:" + TEST_USER_ID), eq(NEW_REFRESH_TOKEN), any(Duration.class));
    }

    @Test
    @DisplayName("실패: Redis에 저장된 토큰과 요청된 토큰이 불일치할 경우 (탈취 의심)")
    void reissueToken_Failure_TokenMismatch() {
        // Given
        final String DIFFERENT_STORED_TOKEN = "oldAndDifferentToken";

        doNothing().when(jwtUtil).validateRefreshToken(VALID_REFRESH_TOKEN);
        when(jwtUtil.getUserIdFromToken(VALID_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);

        // Redis에는 다른 토큰이 저장되어 있음 (불일치 상황)
        when(redisService.getValues("RT:" + TEST_USER_ID)).thenReturn(DIFFERENT_STORED_TOKEN);


        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            authService.reissueToken(VALID_REFRESH_TOKEN);
        });

        // 1. 예외 코드 검증
        assertThat(exception.getCode()).isEqualTo(ErrorCode.INVALID_TOKEN);

        // 2. 새로운 토큰이 발급/저장되지 않았는지 확인
        verify(jwtUtil, never()).generateAccessToken(anyLong());
        verify(redisService, never()).setValues(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("실패: RefreshToken이 만료된 경우")
    void reissueToken_Failure_TokenExpired() {
        // Given
        // validateRefreshToken 호출 시 만료 예외를 던지도록 Mock 설정
        doThrow(new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED))
                .when(jwtUtil)
                .validateRefreshToken(VALID_REFRESH_TOKEN);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            authService.reissueToken(VALID_REFRESH_TOKEN);
        });

        // 1. 예외 코드 검증
        assertThat(exception.getCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED);

        // 2. 이후 로직이 호출되지 않았는지 확인
        verify(jwtUtil, never()).getUserIdFromToken(anyString());
        verify(redisService, never()).getValues(anyString());
        verify(jwtUtil, never()).generateAccessToken(anyLong());
    }
}