package com.inhacapstone04.embersentinelserver.camera_edge.controller;

import com.inhacapstone04.embersentinelserver.camera_edge.dto.request.CameraEdgeCreateRequest;
import com.inhacapstone04.embersentinelserver.camera_edge.dto.response.CameraEdgeResponse;
import com.inhacapstone04.embersentinelserver.camera_edge.service.CameraEdgeCommandService;
import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
public class CameraEdgeCommandController {

    private final CameraEdgeCommandService cameraEdgeCommandService;

    /**
     * 1. room에 camera_edge 추가 API
     * API: POST /room/{roomId}/camera-edge
     * 설명: roomId에 해당하는 room에 새로운 카메라 엣지 디바이스를 등록합니다.
     *
     * @param requestingUserId 요청한 사용자의 ID (JWT에서 추출)
     * @param roomId           카메라를 추가할 Room의 ID
     * @param request          등록할 카메라 정보 (UUID, Alias)
     * @return 등록된 카메라 정보 (201 Created)
     */
    @PostMapping("/{roomId}/camera-edge")
    public ResponseEntity<CameraEdgeResponse> createCameraEdge(
            @AuthorizedUser Long requestingUserId,
            @PathVariable Long roomId,
            @Valid @RequestBody CameraEdgeCreateRequest request
    ) {
        CameraEdgeResponse response = cameraEdgeCommandService.createCameraEdge(
                requestingUserId,
                roomId,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
