package com.inhacapstone04.embersentinelserver.room.dto;

import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;

public record RoomMemberDTO(
        Long userId,
        String nickname,
        String profileImageUrl,
        AuthType authType,
        String role // "VIEWER" or "EDITOR" or "ADMIN"
) {
    public static RoomMemberDTO of(User user, MembershipRole role) {
        return new RoomMemberDTO(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getAuthType(),
                role.name()
        );
    }
}
