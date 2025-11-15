package com.inhacapstone04.embersentinelserver.room.dto;

import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.user.entity.User;

public record MembershipDTO(
        Long userId,
        String nickname,
        String profileImageUrl,
        String role
) {
    public static MembershipDTO of(UserRoomMembership membership) {
        User user = membership.getUser();
        return new MembershipDTO(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                membership.getRole().name() // MembershipRole Enum을 String으로 변환
        );
    }
}
