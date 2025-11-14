package com.inhacapstone04.embersentinelserver.building.controller;

import com.inhacapstone04.embersentinelserver.building.dto.ListBuildingResponse;
import com.inhacapstone04.embersentinelserver.building.dto.SingleBuildingResponse;
import com.inhacapstone04.embersentinelserver.building.service.BuildingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/building")
public class BuildingQueryController {

    private final BuildingQueryService buildingQueryService;

    @GetMapping("/{id}")
    public ResponseEntity<SingleBuildingResponse> getBuildingById(@PathVariable Long id) {

        return ResponseEntity.ok(buildingQueryService.findById(id));
    }

    @GetMapping("/list")
    public ResponseEntity<ListBuildingResponse> getBuildingList() {

        return ResponseEntity.ok(buildingQueryService.findAll());
    }
}
