package com.inhacapstone04.embersentinelserver.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomCreateRequest(

        @NotNull(message = "생성할 Room에 대한 building Id 정보가 없습니다.")
        Long buildingId,
        @NotBlank(message = "생성할 Room에 대한 roomAlias 정보가 없습니다.")
        String roomAlias,
        @NotBlank(message = "생성할 Room에 대한 floor 정보가 없습니다.")
        String floor,
        @NotBlank(message = "생성할 Room에 대한 roomNumber 정보가 없습니다.")
        String roomNumber

) {

}
