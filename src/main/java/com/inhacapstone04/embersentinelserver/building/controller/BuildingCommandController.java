package com.inhacapstone04.embersentinelserver.building.controller;

import com.inhacapstone04.embersentinelserver.building.dto.BuildingCreateRequest;
import com.inhacapstone04.embersentinelserver.building.dto.BuildingDeleteRequest;
import com.inhacapstone04.embersentinelserver.building.dto.BuildingUpdateRequest;
import com.inhacapstone04.embersentinelserver.building.dto.SingleBuildingResponse;
import com.inhacapstone04.embersentinelserver.building.service.BuildingCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/building")
public class BuildingCommandController {
    private final BuildingCommandService buildingCommandService;

    @PostMapping
    public ResponseEntity<SingleBuildingResponse> insertBuildingInfo(
            @RequestBody BuildingCreateRequest buildingCreateRequest
            ) {

        return ResponseEntity.ok(buildingCommandService.createBuilding(buildingCreateRequest));
    }

    @PatchMapping
    public ResponseEntity<SingleBuildingResponse> updateBuildingInfo(
        @RequestBody BuildingUpdateRequest buildingUpdateRequest
    ) {

        return ResponseEntity.ok(buildingCommandService.updateBuildingById(buildingUpdateRequest));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteBuildingInfo(
            @RequestBody BuildingDeleteRequest buildingDeleteRequest
    ) {

        buildingCommandService.deleteBuildingById(buildingDeleteRequest);
        return ResponseEntity.noContent().build();
    }
}
