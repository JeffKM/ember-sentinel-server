package com.inhacapstone04.embersentinelserver.room.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RoomListSummaryRequest(
        @NotEmpty(message = "조회할 Room ID 리스트는 비어있을 수 없습니다.")
        List<Long> roomIds
) {
}
