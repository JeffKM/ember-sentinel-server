package com.inhacapstone04.embersentinelserver.user.dto;

import com.inhacapstone04.embersentinelserver.user.entity.User;

public record UserLoginResultDTO(
        User user,
        boolean isNewUser
) {
    public static UserLoginResultDTO of(User user, boolean isNewUser) {
        return new UserLoginResultDTO(user, isNewUser);
    }
}
