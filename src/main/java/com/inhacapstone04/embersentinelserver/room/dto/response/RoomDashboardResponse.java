package com.inhacapstone04.embersentinelserver.room.dto.response;

import com.inhacapstone04.embersentinelserver.room.dto.RoomStatisticsDTO;

import java.util.List;

public record RoomDashboardResponse(
        long totalRoomCount,
        long totalCameraCount,
        long liveStreamCount,
        List<RoomStatisticsDTO> roomList
) {
    public static RoomDashboardResponse of(List<RoomStatisticsDTO> roomList) {

        // roomList의 'cameraCountPerRoom' 값을 모두 더함
        long totalCameraCount = roomList.stream()
                .mapToLong(RoomStatisticsDTO::cameraCountPerRoom)
                .sum();

        // roomList의 'fireEventCountPerRoom' (LIVE 상태인 것) 값을 모두 더함
        long liveStreamCount = roomList.stream()
                .mapToLong(RoomStatisticsDTO::fireEventCountPerRoom)
                .sum();

        // record의 생성자를 호출하여 최종 객체 반환
        return new RoomDashboardResponse(
                roomList.size(), // 총 방의 개수
                totalCameraCount,
                liveStreamCount,
                roomList
        );
    }
}
