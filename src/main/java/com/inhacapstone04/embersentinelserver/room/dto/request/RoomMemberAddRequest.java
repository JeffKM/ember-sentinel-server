package com.inhacapstone04.embersentinelserver.room.dto.request;

import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomMemberAddRequest(
        @NotBlank @Email
        String userEmail, // 방에 추가할 사용자의 이메일

        @NotNull
        MembershipRole role // 추가할 사용자의 역할
) {
}
