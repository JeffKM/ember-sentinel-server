package com.inhacapstone04.embersentinelserver.building.dto;

import jakarta.validation.constraints.NotNull;

public record BuildingCreateRequest(

        @NotNull(message = "생성할 building의 이름을 입력해주세요.")
        String buildingName

) {

}