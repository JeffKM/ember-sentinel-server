package com.inhacapstone04.embersentinelserver.user.dto.request;

import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import jakarta.validation.constraints.NotNull;

public record PushAlarmRequest(
        @NotNull String email,
        @NotNull AuthType authType,
        @NotNull String alertBody
) {
}
