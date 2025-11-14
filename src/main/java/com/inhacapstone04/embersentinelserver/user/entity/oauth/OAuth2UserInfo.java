package com.inhacapstone04.embersentinelserver.user.entity.oauth;

import com.inhacapstone04.embersentinelserver.user.entity.AuthType;

public interface OAuth2UserInfo {
    String getEmail();
    String getNickname();
    String getProfileImageUrl();
    AuthType getAuthType(); // 어느 제공자인지
}
