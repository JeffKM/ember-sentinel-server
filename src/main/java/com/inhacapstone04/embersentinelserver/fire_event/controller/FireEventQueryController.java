package com.inhacapstone04.embersentinelserver.fire_event.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.FireEventSimpleDTO;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventDetailResponse;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
public class FireEventQueryController {

    private final FireEventQueryService fireEventQueryService;

    /**
     * 1. fire_event에 대한 요약 정보 조회 (상세 조회)
     * API: GET /room/{roomId}/fire-event/{eventId}
     * 설명: 특정 화재 탐지 이벤트(eventId)에 대한 상세 정보를 연관된 스트림, 녹화 파일과 함께 조회합니다.
     */
    @GetMapping("/{roomId}/fire-event/{eventId}")
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
    @GetMapping("/{roomId}/camera_edge/{cameraEdgeId}/fire-event/list")
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
}