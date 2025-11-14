package com.inhacapstone04.embersentinelserver.building.service;

import com.inhacapstone04.embersentinelserver.building.dto.BuildingCreateRequest;
import com.inhacapstone04.embersentinelserver.building.dto.BuildingDeleteRequest;
import com.inhacapstone04.embersentinelserver.building.dto.BuildingUpdateRequest;
import com.inhacapstone04.embersentinelserver.building.dto.SingleBuildingResponse;
import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.inhacapstone04.embersentinelserver.common.exception.ErrorCode.NOT_FOUND_BY_ID;

@Service
@RequiredArgsConstructor
public class BuildingCommandService {
    private final BuildingRepository buildingRepository;

    public SingleBuildingResponse createBuilding(BuildingCreateRequest buildingCreateRequest) {

        return SingleBuildingResponse.of(buildingRepository.save(new Building(buildingCreateRequest.buildingName())));
    }

    public SingleBuildingResponse updateBuildingById(BuildingUpdateRequest buildingUpdateRequest) {
        Building foundBuilding = buildingRepository.findById(buildingUpdateRequest.id()).orElseThrow(
                () -> new CustomException(NOT_FOUND_BY_ID));

        foundBuilding.setBuildingName(buildingUpdateRequest.buildingName());

        return SingleBuildingResponse.of(buildingRepository.save(foundBuilding));
    }

    public void deleteBuildingById(BuildingDeleteRequest buildingDeleteRequest) {

        Building buildingToDelete = buildingRepository.findById(buildingDeleteRequest.id())
                .orElseThrow(() -> new CustomException(NOT_FOUND_BY_ID));

        buildingRepository.delete(buildingToDelete);
    }
}
