package com.inhacapstone04.embersentinelserver.user.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.RedisService;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import com.inhacapstone04.embersentinelserver.user.config.OAuth2ClientProvider;
import com.inhacapstone04.embersentinelserver.user.dto.response.AuthInfoResponse;
import com.inhacapstone04.embersentinelserver.user.dto.UserLoginResultDTO;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.OAuth2UserInfo;
import com.inhacapstone04.embersentinelserver.user.service.oauth.OAuth2ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

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
     */
    public AuthInfoResponse reissueToken(String refreshToken) {

        // 1. Refresh Token 유효성 검사 (만료 여부 포함)
        // Refresh Token이 만료된 경우 CustomException(REFRESH_TOKEN_EXPIRED)를 던짐
        jwtUtil.validateRefreshToken(refreshToken);

        // 2. Refresh Token에서 userId 추출
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        // 3. 해당 userId로 저장된 Refresh Token이 클라이언트가 보낸 토큰과 일치하는지 확인해야 합니다.
        //    불일치 시 throw new CustomException(ErrorCode.INVALID_TOKEN);
        String storedToken = redisService.getValues("RT:" + userId);
        if (!refreshToken.equals(storedToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Token mismatch. Possible theft attempt.");
        }

        // 4. 새 Access Token 발급
        String newAccessToken = jwtUtil.generateAccessToken(userId);

        // 5. 새 Refresh Token 발급 (Rotation)
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // 6. 새로 발급된 Refresh Token을 DB/Redis에 저장(업데이트)해야 합니다.
        redisService.setValues("RT:" + userId, newRefreshToken, Duration.ofMillis(refreshTokenExpirationTime));

        // 7. 응답 DTO 생성
        Long expiresInSeconds = accessTokenExpirationMs / 1000;

        // 재발급이므로 isNewUser는 false
        return AuthInfoResponse.of(newAccessToken, newRefreshToken, expiresInSeconds, false);
    }
}
