package com.inhacapstone04.embersentinelserver.user.config;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.service.oauth.OAuth2ClientService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuth2ClientProvider {

    // 모든 OAuth2Client 빈을 주입받아 Map으로 관리
    private final Map<AuthType, OAuth2ClientService> clients;

    public OAuth2ClientProvider(List<OAuth2ClientService> clients) {
        this.clients = clients.stream()
                .collect(Collectors.toUnmodifiableMap(OAuth2ClientService::getAuthType, Function.identity()));
    }

    /**
     * AuthType에 맞는 클라이언트 구현체를 반환
     */
    public OAuth2ClientService getClient(AuthType authType) {
        OAuth2ClientService client = clients.get(authType);
        if (client == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 소셜 로그인 타입입니다: " + authType);
        }
        return client;
    }
}
