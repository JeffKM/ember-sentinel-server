package com.inhacapstone04.embersentinelserver.room.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.room.dto.request.RoomCreateRequest;
import com.inhacapstone04.embersentinelserver.room.dto.response.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.service.RoomCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * [DELETE] /room/{roomId}
     * roomId에 해당하는 방을 삭제합니다.
     * 요청자가 해당 방의 EDITOR 권한 이상인지 서비스에서 검증합니다.
     *
     * @param userId 요청한 유저 ID (JWT 추출)
     * @param roomId 삭제할 방 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @AuthorizedUser Long userId,
            @PathVariable Long roomId
    ) {
        roomCommandService.deleteRoom(userId, roomId);
        return ResponseEntity.noContent().build();
    }
}
