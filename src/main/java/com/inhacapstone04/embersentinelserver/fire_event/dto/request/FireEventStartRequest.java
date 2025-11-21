package com.inhacapstone04.embersentinelserver.fire_event.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FireEventStartRequest(
        @NotBlank
        String deviceUuid // 라즈베리파이의 고유 식별자 (UUID)
) {
}
