package com.inhacapstone04.embersentinelserver.user.dto;

import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.UserRole;

public record UserInfoResponse(
        String email,
        String nickname,
        String profileImageUrl,
        UserRole userRole,
        AuthType authType,
        String endpointArn
) {
    public static UserInfoResponse of(User user) {
        return new UserInfoResponse(
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getUserRole(),
                user.getAuthType(),
                user.getEndpointArn()
        );
    }
}
