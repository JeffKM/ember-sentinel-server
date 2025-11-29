package com.inhacapstone04.embersentinelserver.media.dto.response;

import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;

import java.time.OffsetDateTime;

public record MediaRecordResponse(
        Long id,
        String s3PresignedUrl,
        OffsetDateTime createdAt
) {
    public static MediaRecordResponse of(MediaRecord record, String presignedUrl) {
        if (record == null) {
            return null;
        }
        return new MediaRecordResponse(
                record.getId(),
                presignedUrl,
                record.getCreatedAt()
        );
    }
}
