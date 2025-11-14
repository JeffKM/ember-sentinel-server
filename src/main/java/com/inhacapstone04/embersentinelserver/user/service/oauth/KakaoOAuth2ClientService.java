package com.inhacapstone04.embersentinelserver.user.service.oauth;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.KakaoUserInfo;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOAuth2ClientService implements OAuth2ClientService {

    private final WebClient webClient;

    @Value("${oauth.kakao.userinfo-uri}")
    private String KAKAO_USERINFO_URL;

    @Override
    public OAuth2UserInfo getUserInfo(String accessToken) {
        Map<String, Object> attributes = webClient.get()
                .uri(KAKAO_USERINFO_URL)
                .headers(headers -> headers.setBearerAuth(accessToken)) // Bearer 토큰 설정
                .retrieve()
                // 4xx, 5xx 에러 처리
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new CustomException(ErrorCode.INVALID_TOKEN, "Kakao 토큰 검증 실패: " + errorBody)
                                ))
                )
                // Map<String, Object> 타입으로 응답 바디를 받음
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block(); // 동기식으로 결과를 기다림

        if (attributes == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Kakao 사용자 정보를 가져오지 못했습니다.");
        }

        return new KakaoUserInfo(attributes);
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.KAKAO;
    }
}
