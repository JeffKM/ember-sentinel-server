package com.inhacapstone04.embersentinelserver.room.dto;

import com.inhacapstone04.embersentinelserver.room.entity.Room;

public record SingleRoomResponse(
        Long roomId,
        String roomAlias,
        String buildingName,
        String floor,
        String roomNumber
) {
    public static SingleRoomResponse of(Room room, String buildingName) {
        return new SingleRoomResponse(
                room.getId(),
                room.getRoomAlias(),
                buildingName,
                room.getBuildingLocationFloor(),
                room.getRoomNumber()
        );
    }
}
