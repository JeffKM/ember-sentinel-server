package com.inhacapstone04.embersentinelserver.building.dto;

import jakarta.validation.constraints.NotNull;

public record BuildingDeleteRequest(

        @NotNull(message = "삭제할 building의 id를 입력해주세요.")
        Long id

) {
}
