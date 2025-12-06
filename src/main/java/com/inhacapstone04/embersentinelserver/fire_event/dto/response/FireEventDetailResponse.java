package com.inhacapstone04.embersentinelserver.fire_event.dto.response;

import com.inhacapstone04.embersentinelserver.camera_edge.dto.CameraEdgeDTO;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.media.dto.MediaRecordDTO;
import com.inhacapstone04.embersentinelserver.media.dto.MediaStreamDTO;

import java.time.OffsetDateTime;

public record FireEventDetailResponse(
        Long id,
        String detectinoType,
        String fireCause,
        Long riskRank,
        OffsetDateTime createdAt,
        CameraEdgeDTO cameraInfo,
        MediaStreamDTO streamInfo,
        MediaRecordDTO recordInfo
) {
    public static FireEventDetailResponse of(FireEvent event, CameraEdgeDTO cameraInfo, MediaStreamDTO streamInfo, MediaRecordDTO recordInfo) {
        return new FireEventDetailResponse(
                event.getId(),
                event.getDetectionType().name(),
                event.getFireCause() != null ? event.getFireCause().name() : "UNKNOWN",
                event.getRiskRank(),
                event.getCreatedAt(),
                cameraInfo,
                streamInfo,
                recordInfo
        );
    }
}
