package com.inhacapstone04.embersentinelserver.auth;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.RedisService;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import com.inhacapstone04.embersentinelserver.support.IntegrationTestSupport;
import com.inhacapstone04.embersentinelserver.user.dto.request.EmailLoginRequest;
import com.inhacapstone04.embersentinelserver.user.dto.response.AuthInfoResponse;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthService 통합 테스트 (실제 Redis)")
class AuthServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired private AuthService authService;
    @Autowired private RedisService redisService;
    @Autowired private JwtUtil jwtUtil;

    @Test
    @DisplayName("이메일 로그인 → 토큰 재발급 → 이전 토큰 블랙리스트 등록 검증")
    void tokenRotationAndBlacklist() {
        // 1. 이메일 로그인
        EmailLoginRequest loginRequest = new EmailLoginRequest("rotation@test.com", "TestUser");
        AuthInfoResponse loginResponse = authService.loginByEmail(AuthType.EMAIL, loginRequest);

        assertThat(loginResponse.accessToken()).isNotNull();
        assertThat(loginResponse.refreshToken()).isNotNull();

        String firstRefreshToken = loginResponse.refreshToken();

        // 2. Redis에 RT가 저장되었는지 확인
        Long userId = jwtUtil.getUserIdFromToken(firstRefreshToken);
        String storedToken = redisService.getValues("RT:" + userId);
        assertThat(storedToken).isEqualTo(firstRefreshToken);

        // 3. 토큰 재발급 (Rotation)
        AuthInfoResponse reissueResponse = authService.reissueToken(firstRefreshToken);
        assertThat(reissueResponse.refreshToken()).isNotEqualTo(firstRefreshToken);

        // 4. 이전 토큰으로 다시 재발급 시도 → 블랙리스트에 의해 차단
        assertThatThrownBy(() -> authService.reissueToken(firstRefreshToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REFRESH_TOKEN_REUSED);

        // 5. 모든 토큰이 무효화되었는지 확인
        assertThat(redisService.getValues("RT:" + userId)).isNull();
    }

    @Test
    @DisplayName("이메일 로그인 → 신규 유저 isNewUser=true")
    void emailLogin_NewUser() {
        // Given
        EmailLoginRequest request = new EmailLoginRequest("new@test.com", "NewUser");

        // When
        AuthInfoResponse response = authService.loginByEmail(AuthType.EMAIL, request);

        // Then
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.grantType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("이메일 로그인 → 기존 유저 isNewUser=false")
    void emailLogin_ExistingUser() {
        // Given
        EmailLoginRequest request = new EmailLoginRequest("existing@test.com", "ExistingUser");
        authService.loginByEmail(AuthType.EMAIL, request); // 첫 번째 로그인

        // When
        AuthInfoResponse response = authService.loginByEmail(AuthType.EMAIL, request); // 두 번째 로그인

        // Then
        assertThat(response.isNewUser()).isFalse();
    }
}
