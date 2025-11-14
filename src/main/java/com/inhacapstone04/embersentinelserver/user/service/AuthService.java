package com.inhacapstone04.embersentinelserver.user.service;

import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import com.inhacapstone04.embersentinelserver.user.config.OAuth2ClientProvider;
import com.inhacapstone04.embersentinelserver.user.dto.AuthInfoResponse;
import com.inhacapstone04.embersentinelserver.user.dto.UserLoginResultDTO;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.OAuth2UserInfo;
import com.inhacapstone04.embersentinelserver.user.service.oauth.OAuth2ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCommandService userCommandService;
    private final JwtUtil jwtUtil;
    private final OAuth2ClientProvider oAuth2ClientProvider;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

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

        // 4. 응답 DTO 생성 (요청 스펙에 맞게 만료 시간을 '초' 단위로 변환)
        Long expiresInSeconds = accessTokenExpirationMs / 1000;

        return AuthInfoResponse.of(serverAccessToken, serverRefreshToken, expiresInSeconds, isNewUser);
    }
}
