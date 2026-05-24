package com.inhacapstone04.embersentinelserver.media.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.media.dto.response.MediaRecordResponse;
import com.inhacapstone04.embersentinelserver.media.service.MediaRecordQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fire-event")
public class MediaRecordQueryController {

    private final MediaRecordQueryService mediaRecordQueryService;

    /**
     * 특정 화재 이벤트의 녹화 파일 조회 API
     * API: GET /fire-event/{fireEventId}/record
     */
    @GetMapping("/{fireEventId}/record")
    public ResponseEntity<MediaRecordResponse> getMediaRecord(
            @AuthorizedUser Long userId,
            @PathVariable Long fireEventId,
            @RequestParam Long roomId
    ) {
        MediaRecordResponse response = mediaRecordQueryService.getMediaRecord(
                userId,
                roomId,
                fireEventId
        );
        return ResponseEntity.ok(response);
    }
}
