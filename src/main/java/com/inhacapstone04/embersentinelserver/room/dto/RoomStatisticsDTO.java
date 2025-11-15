package com.inhacapstone04.embersentinelserver.room.dto;

import com.inhacapstone04.embersentinelserver.room.entity.Room;

public record RoomStatisticsDTO(
        Long roomId,
        String roomAlias,
        String buildingName, // building.building_name
        String floor, // room.building_location_floor
        String roomNumber, // room.room_number
        Long cameraCountPerRoom,
        Long fireEventCountPerRoom
) {
    public static RoomStatisticsDTO of(Room room, String buildingName, Long cameraCountPerRoom, Long fireEventCountPerRoom) {
        return new RoomStatisticsDTO(
                room.getId(),
                room.getRoomAlias(),
                buildingName,
                room.getBuildingLocationFloor(),
                room.getRoomNumber(),
                cameraCountPerRoom,
                fireEventCountPerRoom
        );
    }
}
