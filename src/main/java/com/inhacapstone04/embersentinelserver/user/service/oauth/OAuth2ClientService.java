package com.inhacapstone04.embersentinelserver.user.service.oauth;

import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.OAuth2UserInfo;

public interface OAuth2ClientService {
    /**
     * 3rd-party access token을 받아 사용자 정보를 반환
     */
    OAuth2UserInfo getUserInfo(String accessToken);

    /**
     * 이 클라이언트가 어떤 AuthType을 지원하는지 반환
     */
    AuthType getAuthType();
}
