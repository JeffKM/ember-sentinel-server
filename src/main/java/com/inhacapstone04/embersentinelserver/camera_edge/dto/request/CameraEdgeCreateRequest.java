package com.inhacapstone04.embersentinelserver.camera_edge.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CameraEdgeCreateRequest(
        @NotBlank
        String deviceUuid, // 라즈베리 파이 기기의 고유 ID

        @NotBlank
        String cameraEdgeAlias // "101호-천장캠-01"과 같은 별칭
) {
}
