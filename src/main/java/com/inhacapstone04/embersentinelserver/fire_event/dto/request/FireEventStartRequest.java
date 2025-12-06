package com.inhacapstone04.embersentinelserver.fire_event.dto.request;

import com.inhacapstone04.embersentinelserver.fire_event.entity.DetectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FireEventStartRequest(
        @NotBlank
        String deviceUuid, // 라즈베리파이의 고유 식별자 (UUID)
        @NotNull(message = "감지 유형(FIRE, SMOKE)은 필수입니다.")
        DetectionType detectionType
) {
}
