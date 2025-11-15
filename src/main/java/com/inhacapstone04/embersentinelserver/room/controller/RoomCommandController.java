package com.inhacapstone04.embersentinelserver.room.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.room.dto.RoomCreateRequest;
import com.inhacapstone04.embersentinelserver.room.dto.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.service.RoomCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
public class RoomCommandController {

    private final RoomCommandService roomCommandService;

    /**
     * [POST] /room
     * 새로운 Room을 생성하고, 요청한 유저를 'EDITOR'로 등록합니다.
     *
     * @param userId (@AuthorizedUser 어노테이션을 통해 리졸버가 주입)
     * @param request (@Valid를 통해 buildingId, roomAlias 등이 비어있지 않은지 검증)
     * @return ResponseEntity<RoomSimpleResponse> (201 Created)
     */
    @PostMapping
    public ResponseEntity<SingleRoomResponse> createRoom(
            @AuthorizedUser Long userId,
            @Valid @RequestBody RoomCreateRequest request
    ) {

        SingleRoomResponse response = roomCommandService.createRoom(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
