package com.inhacapstone04.embersentinelserver.user.entity.oauth;

import com.inhacapstone04.embersentinelserver.user.entity.AuthType;

import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        // Google은 'nickname'이 없고 'name'을 제공합니다.
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.GOOGLE;
    }
}
