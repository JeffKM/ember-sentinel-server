package com.inhacapstone04.embersentinelserver.media.controller;

import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventStreamInfoResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventWatchResponse;
import com.inhacapstone04.embersentinelserver.media.service.MediaStreamTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(name = "livekit.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@RequestMapping("/media/test")
public class MediaStreamTestController {

    private final MediaStreamTestService mediaStreamTestService;

    /**
     * LiveKit 스트리밍 생성 및 토큰 발급 테스트 API
     * DB 저장 없이 LiveKit 서버와의 통신만 확인합니다.
     */
    @PostMapping("/stream")
    public ResponseEntity<FireEventStreamInfoResponse> startTestStream() {
        FireEventStreamInfoResponse response = mediaStreamTestService.testStartStreaming();
        return ResponseEntity.ok(response);
    }

    /**
     * [추가됨] 테스트용 시청자(Subscriber) 토큰 발급 API
     * 특정 roomName에 대해 시청 권한만 있는 토큰을 발급합니다.
     * API: GET /media/test/stream/{roomName}/subscribe
     */
    @GetMapping("/stream/{roomName}/subscribe")
    public ResponseEntity<FireEventWatchResponse> getTestSubscribeToken(@PathVariable String roomName) {
        FireEventWatchResponse response = mediaStreamTestService.testSubscribe(roomName);
        return ResponseEntity.ok(response);
    }

    /**
     * [추가됨] 테스트 스트리밍 종료 API
     * 생성된 테스트 방을 삭제하고 Egress를 종료합니다.
     * API: DELETE /media/test/stream/{roomName}
     */
    @DeleteMapping("/stream/{roomName}")
    public ResponseEntity<String> stopTestStream(@PathVariable String roomName) {
        mediaStreamTestService.testStopStreaming(roomName);
        return ResponseEntity.ok("Test stream stopped: " + roomName);
    }
}
