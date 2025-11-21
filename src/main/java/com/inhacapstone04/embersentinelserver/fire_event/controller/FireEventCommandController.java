package com.inhacapstone04.embersentinelserver.fire_event.controller;

import com.inhacapstone04.embersentinelserver.fire_event.dto.request.FireEventStartRequest;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventStreamInfoResponse;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FireEventCommandController {

    private final FireEventCommandService fireEventCommandService;

    /**
     * 1. 화재 감지 및 스트리밍 시작 요청 (라즈베리파이 호출)
     * API: POST /embedded/fire-event/publish
     */
    @PostMapping("/embedded/fire-event/publish")
    public ResponseEntity<FireEventStreamInfoResponse> startFireEvent(
            @Valid @RequestBody FireEventStartRequest request
    ) {
        FireEventStreamInfoResponse response = fireEventCommandService.startFireEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
