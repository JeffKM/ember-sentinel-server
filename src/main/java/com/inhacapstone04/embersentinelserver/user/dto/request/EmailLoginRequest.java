package com.inhacapstone04.embersentinelserver.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record EmailLoginRequest(
        @NotNull String email,
        @NotNull String nickname
) {
}
