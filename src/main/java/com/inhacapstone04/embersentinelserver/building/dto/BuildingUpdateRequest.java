package com.inhacapstone04.embersentinelserver.building.dto;

import jakarta.validation.constraints.NotNull;

public record BuildingUpdateRequest(

        @NotNull(message = "수정할 building의 id를 입력해주세요.")
        Long id,

        @NotNull(message = "수정할 building의 새로운 이름을 입력해주세요.")
        String buildingName
) {

}
