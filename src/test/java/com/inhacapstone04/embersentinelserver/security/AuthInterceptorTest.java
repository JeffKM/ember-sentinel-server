package com.inhacapstone04.embersentinelserver.security;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import com.inhacapstone04.embersentinelserver.security.interceptor.AuthInterceptor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest // @Autowired로 실제 빈들을 주입받기 위해 사용
@DisplayName("AuthInterceptor 테스트")
class AuthInterceptorTest {

    @Autowired
    private AuthInterceptor authInterceptor; // 테스트 대상

    @Autowired
    private JwtUtil jwtUtil; // 실제 토큰 생성을 위해 주입

    // 만료된 토큰 생성을 위해, JwtUtil이 사용하는 것과 동일한 키가 필요
    @Value("${jwt.secret-key}")
    private String secret;
    private Key secretKey;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Object handler; // preHandle의 3번째 인자 (dummy)

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 Mock 요청/응답 객체를 초기화
        this.request = new MockHttpServletRequest();
        this.response = new MockHttpServletResponse();
        this.handler = new Object(); // 인터셉터 로직에서 사용되지 않으므로 dummy 객체
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 요청 시 preHandle 성공 (true 반환)")
    void preHandle_WithValidToken_ShouldReturnTrue() throws Exception {
        // given
        Long userId = 1L;
        String validToken = jwtUtil.generateAccessToken(userId);

        // Mock 요청 헤더에 유효한 토큰 추가
        request.addHeader("Authorization", "Bearer " + validToken);

        // when
        boolean result = authInterceptor.preHandle(request, response, handler);

        // then
        // 1. preHandle은 true를 반환해야 함
        assertThat(result).isTrue();

        // 2. request attribute에 "userId"가 정확히 저장되어야 함
        Object userIdAttribute = request.getAttribute("userId");
        assertThat(userIdAttribute).isNotNull();
        assertThat(userIdAttribute).isInstanceOf(Long.class);
        assertThat((Long) userIdAttribute).isEqualTo(userId);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 EMPTY_TOKEN 예외 발생")
    void preHandle_WithoutHeader_ShouldThrowEmptyTokenException() {
        // given
        // (헤더를 추가하지 않음)

        // when & then
        // preHandle 실행 시 CustomException(EMPTY_TOKEN)이 발생하는지 검증
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EMPTY_TOKEN.getCode());
    }

    @Test
    @DisplayName("Bearer 접두사가 없으면 EMPTY_TOKEN 예외 발생")
    void preHandle_WithoutBearerPrefix_ShouldThrowEmptyTokenException() {
        // given
        String token = jwtUtil.generateAccessToken(1L);
        request.addHeader("Authorization", token); // "Bearer " 누락

        // when & then
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EMPTY_TOKEN.getCode());
    }

    @Test
    @DisplayName("토큰이 비어있으면 (Bearer ) EMPTY_TOKEN 예외 발생")
    void preHandle_WithEmptyToken_ShouldThrowEmptyTokenException() {
        // given
        request.addHeader("Authorization", "Bearer "); // 토큰 값 없음

        // when & then
        // (JwtUtil.validateToken이 EMPTY_TOKEN을 throw)
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EMPTY_TOKEN.getCode());
    }

    @Test
    @DisplayName("유효하지 않은 형식의 토큰이면 INVALID_TOKEN 예외 발생")
    void preHandle_WithInvalidTokenFormat_ShouldThrowInvalidTokenException() {
        // given
        request.addHeader("Authorization", "Bearer this.is.invalid");

        // when & then
        // (JwtUtil.validateToken이 INVALID_TOKEN을 throw)
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN.getCode());
    }

    @Test
    @DisplayName("만료된 토큰이면 ACCESS_TOKEN_EXPIRED 예외 발생")
    void preHandle_WithExpiredToken_ShouldThrowExpiredException() {
        // given
        // 'secretKey'를 사용해 수동으로 만료된 토큰 생성
        String expiredToken = Jwts.builder()
                .claim("userId", 1L)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000)) // 2초 전 발급
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 1초 전 만료
                .signWith(secretKey)
                .compact();

        request.addHeader("Authorization", "Bearer " + expiredToken);

        // when & then
        // (JwtUtil.validateToken이 ACCESS_TOKEN_EXPIRED를 throw)
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ACCESS_TOKEN_EXPIRED.getCode());
    }
}
