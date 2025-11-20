package com.inhacapstone04.embersentinelserver.media.dto;

import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;

import java.time.OffsetDateTime;

public record MediaRecordDTO(
        Long id,
        String fileUrl,
        OffsetDateTime createdAt
) {
    // Entity -> DTO 변환 메서드는 MediaRecord 엔티티 구조에 맞춰 추가 필요
    public static MediaRecordDTO of(MediaRecord record) {
        if (record == null) {
            return null;
        }
        return new MediaRecordDTO(
                record.getId(),
                record.getS3BucketPath(),
                record.getCreatedAt()
        );
    }
}
