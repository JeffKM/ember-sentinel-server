package com.inhacapstone04.embersentinelserver.security;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest // Spring 컨텍스트를 로드하여 @Value 속성 주입
@DisplayName("JwtUtil 테스트")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    private Long testUserId;
    private Key testWrongKey; // 잘못된 서명 키 (테스트용)

    @BeforeEach
    void setUp() {
        testUserId = 12345L;

        // JwtUtil의 secret-key와 다른 임의의 키 생성
        String wrongSecret = "ThisIsDefinitelyTheWrongSecretKeyForTestingSignature123";
        testWrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Access Token 생성 및 검증, ID 추출 성공 (Happy Path)")
    void generateAndValidateAccessToken_Success() {
        // given
        // when
        String accessToken = jwtUtil.generateAccessToken(testUserId);

        // then
        assertThat(accessToken).isNotNull().isNotEmpty();

        // 1. 토큰 검증이 예외 없이 통과해야 함
        assertThatCode(() -> jwtUtil.validateToken(accessToken))
                .doesNotThrowAnyException();

        // 2. 토큰에서 추출한 userId가 원본과 동일해야 함
        Long extractedUserId = jwtUtil.getUserIdFromToken(accessToken);
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("Refresh Token 생성 및 검증, ID 추출 성공 (Happy Path)")
    void generateAndValidateRefreshToken_Success() {
        // given
        // when
        String refreshToken = jwtUtil.generateRefreshToken(testUserId);

        // then
        assertThat(refreshToken).isNotNull().isNotEmpty();

        // 1. 토큰 검증이 예외 없이 통과해야 함
        assertThatCode(() -> jwtUtil.validateToken(refreshToken))
                .doesNotThrowAnyException();

        // 2. 토큰에서 추출한 userId가 원본과 동일해야 함
        Long extractedUserId = jwtUtil.getUserIdFromToken(refreshToken);
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("유효하지 않은 형식의 토큰 검증 시 INVALID_TOKEN 예외 발생")
    void validateToken_InvalidFormat() {
        // given
        String invalidToken = "not.a.real.jwt.token";

        // when & then
        // validateToken 호출 시 CustomException이 발생하는지 확인
        assertThatThrownBy(() -> jwtUtil.validateToken(invalidToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN.getCode());
    }

    @Test
    @DisplayName("Null 토큰 검증 시 EMPTY_TOKEN 예외 발생")
    void validateToken_NullToken() {
        // given
        String nullToken = null;

        // when & then
        // (jjwt 라이브러리가 IllegalArgumentException을 던지고, JwtUtil이 EMPTY_TOKEN으로 변환)
        assertThatThrownBy(() -> jwtUtil.validateToken(nullToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EMPTY_TOKEN.getCode());
    }

    @Test
    @DisplayName("빈 문자열 토큰 검증 시 EMPTY_TOKEN 예외 발생")
    void validateToken_EmptyToken() {
        // given
        String emptyToken = "";

        // when & then
        assertThatThrownBy(() -> jwtUtil.validateToken(emptyToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EMPTY_TOKEN.getCode());
    }

    @Test
    @DisplayName("다른 Secret으로 서명된 토큰 검증 시 INVALID_TOKEN 예외 발생")
    void validateToken_WrongSignature() {
        // given
        // 'testWrongKey'를 사용하여 의도적으로 잘못된 토큰을 생성
        String tokenWithWrongKey = Jwts.builder()
                .claim("userId", testUserId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60)) // 1분
                .signWith(testWrongKey) // 다른 키로 서명
                .compact();

        // when & then
        // (jjwt가 SignatureException을 던지고, JwtUtil이 INVALID_TOKEN으로 변환)
        assertThatThrownBy(() -> jwtUtil.validateToken(tokenWithWrongKey))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN.getCode());
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 ACCESS_TOKEN_EXPIRED 예외 발생")
    void validateToken_ExpiredToken() {
        // given
        // 만료 시간을 1ms(거의 즉시 만료)로 설정하여 토큰 생성
        // (주의: @SpringBootTest로 주입된 JwtUtil은 설정된 만료 시간을 따르므로,
        //  이 테스트를 위해서는 리플렉션을 사용하거나, JwtUtil을 리팩토링(만료 시간 주입)해야 함)

        // -> 더 나은 방법: 만료된 토큰을 직접 생성 (리플렉션 불필요)
        // (JwtUtil의 private 메서드를 테스트하는 대신, 만료된 토큰을 수동 생성하여 테스트)
        String expiredToken = Jwts.builder()
                .claim("userId", testUserId)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000)) // 2초 전에 발급
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 1초 전에 만료
                .signWith(getSecretKeyFromJwtUtil()) // (실제 키 사용)
                .compact();

        // when & then
        // (jjwt가 ExpiredJwtException을 던지고, JwtUtil이 ACCESS_TOKEN_EXPIRED로 변환)
        assertThatThrownBy(() -> jwtUtil.validateToken(expiredToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ACCESS_TOKEN_EXPIRED.getCode());
    }


    /**
     * 테스트용 헬퍼 메서드
     * (리플렉션을 사용하여 JwtUtil의 private 'secretKey' 필드에 접근)
     * (만약 'secretKey' 필드에 접근할 수 없다면 이 테스트는 실패합니다.)
     */
    private Key getSecretKeyFromJwtUtil() {
        try {
            java.lang.reflect.Field field = JwtUtil.class.getDeclaredField("secretKey");
            field.setAccessible(true);
            return (Key) field.get(jwtUtil);
        } catch (Exception e) {
            fail("테스트 실패: JwtUtil의 'secretKey' 필드에 접근할 수 없습니다. (리플렉션 실패)", e);
            return null;
        }
    }
}
