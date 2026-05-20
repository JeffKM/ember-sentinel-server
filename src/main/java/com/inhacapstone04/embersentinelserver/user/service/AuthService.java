package com.inhacapstone04.embersentinelserver.user.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.RedisService;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import com.inhacapstone04.embersentinelserver.user.config.OAuth2ClientProvider;
import com.inhacapstone04.embersentinelserver.user.dto.request.EmailLoginRequest;
import com.inhacapstone04.embersentinelserver.user.dto.response.AuthInfoResponse;
import com.inhacapstone04.embersentinelserver.user.dto.UserLoginResultDTO;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.OAuth2UserInfo;
import com.inhacapstone04.embersentinelserver.user.service.oauth.OAuth2ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCommandService userCommandService;
    private final JwtUtil jwtUtil;
    private final OAuth2ClientProvider oAuth2ClientProvider;
    private final RedisService redisService;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationTime;

    /**
     * 소셜 로그인 (및 자동 회원가입)
     * - OAuth 검증
     * - 사용자 조회/생성 (UserCommandService 위임)
     * - JWT 발급
     */
    public AuthInfoResponse login(AuthType authType, String thirdPartyAccessToken) {

        // 1. 3rd-party 토큰 검증 및 사용자 정보 조회
        OAuth2ClientService client = oAuth2ClientProvider.getClient(authType);
        OAuth2UserInfo userInfo = client.getUserInfo(thirdPartyAccessToken);

        // 2. 사용자 조회 또는 신규 등록 (UserCommandService에 위임)
        UserLoginResultDTO loginResult = userCommandService.findOrCreateUser(userInfo);

        User user = loginResult.user();
        boolean isNewUser = loginResult.isNewUser();

        // 3. 서버 자체 JWT 발급
        String serverAccessToken = jwtUtil.generateAccessToken(user.getId());
        String serverRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 4. Refresh Token을 Redis에 저장 (key: "RT:<userId>", value: token, TTL 설정)
        redisService.setValues(
                "RT:" + user.getId(),
                serverRefreshToken,
                Duration.ofMillis(refreshTokenExpirationTime)
        );

        // 5. 응답 DTO 생성 (요청 스펙에 맞게 만료 시간을 '초' 단위로 변환)
        Long expiresInSeconds = accessTokenExpirationMs / 1000;

        return AuthInfoResponse.of(serverAccessToken, serverRefreshToken, expiresInSeconds, isNewUser);
    }

    /**
     * Refresh Token을 사용하여 Access Token과 Refresh Token을 재발급합니다.
     * 이전 토큰은 블랙리스트에 등록되며, 재사용 감지 시 해당 사용자의 모든 토큰을 무효화합니다.
     */
    public AuthInfoResponse reissueToken(String refreshToken) {

        // 1. Refresh Token 유효성 검사 (만료 여부 포함)
        jwtUtil.validateRefreshToken(refreshToken);

        // 2. 블랙리스트 확인 — 재사용 감지
        String tokenHash = hashToken(refreshToken);
        if (redisService.hasKey("BL:" + tokenHash)) {
            // 토큰 재사용 감지: 도용 가능성 → 해당 사용자의 모든 토큰 무효화
            Long userId = jwtUtil.getUserIdFromToken(refreshToken);
            redisService.deleteValues("RT:" + userId);
            log.warn("Refresh Token 재사용 감지 - userId: {}, 모든 토큰 무효화 처리", userId);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        // 3. Refresh Token에서 userId 추출
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        // 4. Redis에 저장된 Refresh Token과 비교
        String storedToken = redisService.getValues("RT:" + userId);
        if (!refreshToken.equals(storedToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Token mismatch. Possible theft attempt.");
        }

        // 5. 새 Access Token 발급
        String newAccessToken = jwtUtil.generateAccessToken(userId);

        // 6. 새 Refresh Token 발급 (Rotation)
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // 7. 새로 발급된 Refresh Token을 Redis에 저장
        redisService.setValues("RT:" + userId, newRefreshToken, Duration.ofMillis(refreshTokenExpirationTime));

        // 8. 이전 Refresh Token을 블랙리스트에 등록 (TTL = 원래 만료 시간)
        redisService.setValues("BL:" + tokenHash, userId.toString(), Duration.ofMillis(refreshTokenExpirationTime));

        // 9. 응답 DTO 생성
        Long expiresInSeconds = accessTokenExpirationMs / 1000;
        return AuthInfoResponse.of(newAccessToken, newRefreshToken, expiresInSeconds, false);
    }

    /**
     * 토큰을 SHA-256으로 해시합니다 (블랙리스트 키 생성용).
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    public AuthInfoResponse loginByEmail(AuthType authType, @Valid EmailLoginRequest request) {
        // 1. 사용자 조회 또는 신규 등록 (UserCommandService에 위임)
        UserLoginResultDTO loginResult = userCommandService.findOrCreateUserByEmail(request);

        User user = loginResult.user();
        boolean isNewUser = loginResult.isNewUser();

        // 2. 서버 자체 JWT 발급
        String serverAccessToken = jwtUtil.generateAccessToken(user.getId());
        String serverRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 3. Refresh Token을 Redis에 저장 (key: "RT:<userId>", value: token, TTL 설정)
        redisService.setValues(
                "RT:" + user.getId(),
                serverRefreshToken,
                Duration.ofMillis(refreshTokenExpirationTime)
        );

        // 4. 응답 DTO 생성 (요청 스펙에 맞게 만료 시간을 '초' 단위로 변환)
        Long expiresInSeconds = accessTokenExpirationMs / 1000;

        return AuthInfoResponse.of(serverAccessToken, serverRefreshToken, expiresInSeconds, isNewUser);
    }
}
