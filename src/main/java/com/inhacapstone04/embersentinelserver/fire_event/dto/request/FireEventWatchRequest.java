package com.inhacapstone04.embersentinelserver.fire_event.dto.request;

import jakarta.validation.constraints.NotNull;

public record FireEventWatchRequest(
        @NotNull
        Long roomId
) {
}
