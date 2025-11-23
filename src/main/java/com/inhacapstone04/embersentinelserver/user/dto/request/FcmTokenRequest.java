package com.inhacapstone04.embersentinelserver.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수 입력 값입니다.")
        String fcmToken
) {}
