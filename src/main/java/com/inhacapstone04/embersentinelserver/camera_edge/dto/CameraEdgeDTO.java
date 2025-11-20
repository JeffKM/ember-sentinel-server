package com.inhacapstone04.embersentinelserver.camera_edge.dto;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;

import java.time.OffsetDateTime;

public record CameraEdgeDTO(
        Long cameraId,
        String deviceUuid,
        String cameraEdgeAlias,
        OffsetDateTime createdAt
) {
    public static CameraEdgeDTO of(CameraEdge cameraEdge) {
        return new CameraEdgeDTO(
                cameraEdge.getId(),
                cameraEdge.getDeviceUuid(),
                cameraEdge.getCameraEdgeAlias(),
                cameraEdge.getCreatedAt()
        );
    }
}
