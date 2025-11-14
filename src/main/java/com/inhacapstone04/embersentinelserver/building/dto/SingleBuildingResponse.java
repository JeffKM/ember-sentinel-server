package com.inhacapstone04.embersentinelserver.building.dto;

import com.inhacapstone04.embersentinelserver.building.entity.Building;

public record SingleBuildingResponse(
        Long id,
        String buildingName
) {
    public static SingleBuildingResponse of(Building building) {
        return new SingleBuildingResponse(building.getId(),
                building.getBuildingName());
    }
}
