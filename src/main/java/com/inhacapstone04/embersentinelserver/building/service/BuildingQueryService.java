package com.inhacapstone04.embersentinelserver.building.service;

import com.inhacapstone04.embersentinelserver.building.dto.ListBuildingResponse;
import com.inhacapstone04.embersentinelserver.building.dto.SingleBuildingResponse;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.inhacapstone04.embersentinelserver.common.exception.ErrorCode.NOT_FOUND_BY_ID;

@Service
@RequiredArgsConstructor
public class BuildingQueryService {
    private final BuildingRepository buildingRepository;

    public SingleBuildingResponse findById(Long id) {

        return SingleBuildingResponse.of(buildingRepository.findById(id).orElseThrow(
                () -> new CustomException(NOT_FOUND_BY_ID)
        ));
    }

    public ListBuildingResponse findAll() {

        return ListBuildingResponse.of(buildingRepository.findAll());
    }
}
