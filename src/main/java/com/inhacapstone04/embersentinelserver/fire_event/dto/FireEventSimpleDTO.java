package com.inhacapstone04.embersentinelserver.fire_event.dto;

import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;

import java.time.OffsetDateTime;

public record FireEventSimpleDTO(
        Long id,
        String fireCause,
        Long riskRank,
        OffsetDateTime createdAt
) {
    public static FireEventSimpleDTO of(FireEvent event) {
        return new FireEventSimpleDTO(
                event.getId(),
                event.getFireCause() != null ? event.getFireCause().name() : "UNKNOWN",
                event.getRiskRank(),
                event.getCreatedAt()
        );
    }
}
