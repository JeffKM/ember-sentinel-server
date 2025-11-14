package com.inhacapstone04.embersentinelserver.user.entity.oauth;

import com.inhacapstone04.embersentinelserver.user.entity.AuthType;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked") // Map 캐스팅을 위함
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;

        // Kakao 응답은 중첩 구조를 가집니다.
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = (Map<String, Object>) kakaoAccount.get("profile");
    }

    @Override
    public String getEmail() {
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getNickname() {
        return (String) profile.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) profile.get("profile_image_url");
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.KAKAO;
    }
}
