package com.inhacapstone04.embersentinelserver.room.dto.response;

import com.inhacapstone04.embersentinelserver.camera_edge.dto.CameraEdgeDTO;
import com.inhacapstone04.embersentinelserver.room.dto.MembershipDTO;
import com.inhacapstone04.embersentinelserver.room.entity.Room;

import java.util.List;
import java.util.stream.Collectors;

public record RoomDetailResponse(
        Long roomId,
        String roomAlias,
        String buildingName,
        String floor,
        String roomNumber,
        List<MembershipDTO> members,
        List<CameraEdgeDTO> cameras
) {
    public static RoomDetailResponse of(Room room, List<CameraEdgeDTO> cameras) {
        // Room 엔티티에 N+1 없이 조회된 멤버 목록을 DTO로 변환
        List<MembershipDTO> members = room.getUserMemberships().stream()
                .map(MembershipDTO::of)
                .collect(Collectors.toList());

        return new RoomDetailResponse(
                room.getId(),
                room.getRoomAlias(),
                room.getBuilding().getBuildingName(),
                room.getBuildingLocationFloor(),
                room.getRoomNumber(),
                members,
                cameras
        );
    }
}
