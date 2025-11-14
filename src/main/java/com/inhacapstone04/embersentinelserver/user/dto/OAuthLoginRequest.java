package com.inhacapstone04.embersentinelserver.user.dto;

import jakarta.validation.constraints.NotNull;

public record OAuthLoginRequest(

        @NotNull(message = "서드 파티의 액세스 토큰을 입력해주세요.")
        String accessToken

) {
}