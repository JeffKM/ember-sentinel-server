package com.inhacapstone04.embersentinelserver.building.dto;

import com.inhacapstone04.embersentinelserver.building.entity.Building;

import java.util.List;

public record ListBuildingResponse(
        List<SingleBuildingResponse> buildingList
) {
    public static ListBuildingResponse of(List<Building> buildings) {
        return new ListBuildingResponse(buildings.stream().map(SingleBuildingResponse::of).toList());
    }
}
