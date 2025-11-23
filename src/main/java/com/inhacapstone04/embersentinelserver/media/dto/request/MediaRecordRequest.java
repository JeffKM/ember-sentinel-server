package com.inhacapstone04.embersentinelserver.media.dto.request;

import jakarta.validation.constraints.NotNull;

public record MediaRecordRequest(
        @NotNull(message = "Room ID는 필수입니다.")
        Long roomId
) {}
