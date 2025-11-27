package com.inhacapstone04.embersentinelserver.camera_edge.dto;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;

public record CameraEdgeWithIsFireDTO(
        Long cameraId,
        String deviceUuid,
        String cameraEdgeAlias,
        String locationFloor,
        String roomNumber,
        boolean isFireOccurring, // 현재 화재 발생 여부
        Long fireEventId // 화재 발생 시 fire_event_id
) {

    /**
     * [중요] RoomRepository의 JPQL 'SELECT NEW' 구문이
     * 이 생성자를 정확히 호출합니다.
     *
     * @param cameraId (c.id)
     * @param deviceUuid (c.deviceUuid)
     * @param cameraEdgeAlias (c.cameraEdgeAlias)
     * @param fireEventId (f.id)
     * @param status (ms.streamingStatus)
     */
    public CameraEdgeWithIsFireDTO(Long cameraId, String deviceUuid, String cameraEdgeAlias, String locationFloor, String roomNumber, Long fireEventId, StreamingStatus status) {
        this(cameraId, deviceUuid, cameraEdgeAlias, locationFloor, roomNumber, (status == StreamingStatus.LIVE), (status == StreamingStatus.LIVE ? fireEventId : null));
    }

    public static CameraEdgeWithIsFireDTO of(CameraEdge cameraEdge, Long fireEventId, StreamingStatus status) {
        return new CameraEdgeWithIsFireDTO(
                cameraEdge.getId(),
                cameraEdge.getDeviceUuid(),
                cameraEdge.getCameraEdgeAlias(),
                cameraEdge.getRoom().getBuildingLocationFloor(),
                cameraEdge.getRoom().getRoomNumber(),
                (status == StreamingStatus.LIVE),
                (status == StreamingStatus.LIVE) ? fireEventId : null
        );
    }
}
