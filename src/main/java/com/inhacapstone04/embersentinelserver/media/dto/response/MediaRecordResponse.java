package com.inhacapstone04.embersentinelserver.media.dto.response;

import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;

import java.time.OffsetDateTime;

public record MediaRecordResponse(
        Long id,
        String s3BucketPath,
        OffsetDateTime createdAt
) {
    public static MediaRecordResponse of(MediaRecord record) {
        if (record == null) {
            return null;
        }
        return new MediaRecordResponse(
                record.getId(),
                record.getS3BucketPath(),
                record.getCreatedAt()
        );
    }
}
