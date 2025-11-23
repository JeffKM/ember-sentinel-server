package com.inhacapstone04.embersentinelserver.fire_event.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.FireEventSimpleDTO;
import com.inhacapstone04.embersentinelserver.fire_event.dto.request.FireEventWatchRequest;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventDetailResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventWatchResponse;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FireEventQueryController {

    private final FireEventQueryService fireEventQueryService;

    /**
     * 1. fire_event에 대한 요약 정보 조회 (상세 조회)
     * API: GET /room/{roomId}/fire-event/{eventId}
     * 설명: 특정 화재 탐지 이벤트(eventId)에 대한 상세 정보를 연관된 스트림, 녹화 파일과 함께 조회합니다.
     */
    @GetMapping("/room/{roomId}/fire-event/{eventId}")
    public ResponseEntity<FireEventDetailResponse> getFireEventDetail(
            @AuthorizedUser Long requestingUserId,
            @PathVariable Long roomId,
            @PathVariable Long eventId
    ) {
        FireEventDetailResponse response = fireEventQueryService.getFireEventDetail(
                requestingUserId,
                roomId,
                eventId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 2. camera_edge가 탐지한 과거 fire_event 목록 조회
     * API: GET /room/{roomId}/camera_edge/{cameraEdgeId}/fire-event/list
     * 설명: ID가 cameraEdgeId인 카메라가 탐지한 과거 화재 이벤트 목록을 페이징하여 조회합니다.
     */
    @GetMapping("/room/{roomId}/camera_edge/{cameraEdgeId}/fire-event/list")
    public ResponseEntity<PageResponse<FireEventSimpleDTO>> getFireEventList(
            @AuthorizedUser Long requestingUserId,
            @PathVariable Long roomId,
            @PathVariable Long cameraEdgeId,
            @PageableDefault(page = 1) Pageable pageable
    ) {
        PageResponse<FireEventSimpleDTO> response = fireEventQueryService.getFireEventList(
                requestingUserId,
                roomId,
                cameraEdgeId,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 3. 화재 상황 스트리밍 시청을 위한 Subscriber Token 발급 요청
     * API: GET /room/fire-event/{fireEventId}/stream/subscribe
     * 설명: 사용자가 화재 영상을 실시간으로 시청하기 위해 필요한 LiveKit 토큰을 발급받습니다.
     * 주의: GET 요청이지만 roomId를 RequestBody로 받습니다.
     */
    @GetMapping("/fire-event/{fireEventId}/stream/subscribe")
    public ResponseEntity<FireEventWatchResponse> getFireEventWatchToken(
            @AuthorizedUser Long requestingUserId,
            @PathVariable Long fireEventId,
            @Valid @RequestBody FireEventWatchRequest request
    ) {
        FireEventWatchResponse response = fireEventQueryService.getFireEventWatchToken(
                requestingUserId,
                request.roomId(),
                fireEventId
        );
        return ResponseEntity.ok(response);
    }
}