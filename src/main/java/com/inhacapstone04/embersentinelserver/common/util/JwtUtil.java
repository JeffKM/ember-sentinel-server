package com.inhacapstone04.embersentinelserver.common.util;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final Key secretKey;
    private final long accessTokenExpirationTime;
    private final long refreshTokenExpirationTime;

    public JwtUtil(
            @Value("${jwt.secret-key}") String secret, // (이전 수정 사항 반영)
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationTime,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationTime
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    public String generateAccessToken(Long userId) {
        return createToken(userId, accessTokenExpirationTime);
    }

    public String generateRefreshToken(Long userId) {
        return createToken(userId, refreshTokenExpirationTime);
    }

    /**
     * 토큰 생성을 위한 private 헬퍼 메서드
     *
     * @param userId         토큰에 담을 유저 ID
     * @param expirationTime 만료 시간 (ms)
     * @return 생성된 JWT 문자열
     */
    private String createToken(Long userId, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .claim("userId", userId)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 Claims를 파싱하는 핵심 메서드 (변경 필요 없음)
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰 유효성 검사 (변경 필요 없음)
     * 유효하지 않으면 CustomException을 throw 하도록 변경
     * AuthInterceptor에서 이 예외를 GlobalExceptionHandler가 처리하도록 함
     */
    public void validateToken(String token) {
        try {
            // getClaims() 자체가 파싱을 수행하며,
            // 만료, 서명 오류 등일 때 예외를 던짐
            getClaims(token);
        } catch (ExpiredJwtException e) {
            // (JWT 파싱 실패, 만료 등)
            throw new CustomException(ErrorCode.ACCESS_TOKEN_EXPIRED);
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException e) {
            // SecurityException, MalformedJwtException, UnsupportedJwtException
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            // 토큰이 비어있거나 인자가 잘못된 경우
            throw new CustomException(ErrorCode.EMPTY_TOKEN, "Token is empty or invalid.");
        } catch (Exception e) {
            // 그 외 모든 인증 실패
            throw new CustomException(ErrorCode.AUTHENTICATION_FAILED);
        }
    }

    /**
     * Refresh Token 유효성 검사
     * 만료 시 REFRESH_TOKEN_EXPIRED 예외를 던져 Access Token과 구분
     */
    public void validateRefreshToken(String token) {
        try {
            getClaims(token);
        } catch (ExpiredJwtException e) {
            // Refresh Token 만료
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED); // 👈 새로운 ErrorCode 필요
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            // 서명/형식/인자 오류
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            // 그 외 모든 인증 실패
            throw new CustomException(ErrorCode.AUTHENTICATION_FAILED);
        }
    }

    /**
     * 토큰에서 userId(Long) 추출 (변경 필요 없음)
     * (Access/Refresh 토큰 모두 동일한 로직으로 추출 가능)
     */
    public Long getUserIdFromToken(String token) {
        try {
            // "userId" 클레임을 Long 타입으로 변환
            return getClaims(token).get("userId", Long.class);
        } catch (CustomException e) {
            // validateToken에서 던진 CustomException을 그대로 다시 던집니다.
            throw e;
        } catch (Exception e) {
            // 클레임 파싱 실패 (예: 'userId'가 없거나 Long 타입이 아님)
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Failed to parse userId from token.");
        }
    }
}