package com.inhacapstone04.embersentinelserver.fire_event.dto.response;

public record FireEventWatchResponse(
        String subscriberToken,
        String livekitRoomName,
        Long fireEventId
) {
    public static FireEventWatchResponse of(String subscriberToken, String livekitRoomName, Long fireEventId) {
        return new FireEventWatchResponse(subscriberToken, livekitRoomName, fireEventId);
    }
}
