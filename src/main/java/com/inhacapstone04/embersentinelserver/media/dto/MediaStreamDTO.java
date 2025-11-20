package com.inhacapstone04.embersentinelserver.media.dto;

import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;

public record MediaStreamDTO(
        Long id, // media_stream.id
        String livekitRoomName, // media_stream.livekit_room_name
        StreamingStatus streamingStatus // media_stream.streaming_status
) {
    public static MediaStreamDTO of(MediaStream stream) {
        if (stream == null) {
            return null;
        }
        return new MediaStreamDTO(
                stream.getId(),
                stream.getLivekitRoomName(),
                stream.getStreamingStatus()
        );
    }
}
