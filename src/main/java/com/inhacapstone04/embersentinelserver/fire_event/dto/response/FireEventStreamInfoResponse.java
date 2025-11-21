package com.inhacapstone04.embersentinelserver.fire_event.dto.response;

public record FireEventStreamInfoResponse(
        String token,           // LiveKit Publisher Token
        String livekitRoomName, // 생성된 스트리밍 방 이름 (Stream Key)
        Long fireEventId        // 생성된 이벤트 ID
) {
    public static FireEventStreamInfoResponse of(String token, String livekitRoomName, Long fireEventId) {
        return new FireEventStreamInfoResponse(token, livekitRoomName, fireEventId);
    }
}
