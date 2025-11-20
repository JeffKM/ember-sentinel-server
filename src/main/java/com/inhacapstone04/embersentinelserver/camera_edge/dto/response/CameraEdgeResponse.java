package com.inhacapstone04.embersentinelserver.camera_edge.dto.response;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;

import java.time.OffsetDateTime;

public record CameraEdgeResponse(
        Long cameraId, // camera_edge.id
        String deviceUuid, // camera_edge.device_guid
        String cameraEdgeAlias, // camera_edge.camera_edge_alias
        OffsetDateTime createdAt, // camera_edge.created_at
        OffsetDateTime modifiedAt // camera_edge.modified_at
) {
    public static CameraEdgeResponse of(CameraEdge entity) {
        return new CameraEdgeResponse(
                entity.getId(),
                entity.getDeviceUuid(),
                entity.getCameraEdgeAlias(),
                entity.getCreatedAt(),
                entity.getModifiedAt()
        );
    }
}
