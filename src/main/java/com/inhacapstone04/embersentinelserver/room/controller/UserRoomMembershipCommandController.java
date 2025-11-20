package com.inhacapstone04.embersentinelserver.room.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.room.dto.RoomMemberResponse;
import com.inhacapstone04.embersentinelserver.room.dto.request.RoomMemberAddRequest;
import com.inhacapstone04.embersentinelserver.room.service.UserRoomMembershipCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Room 멤버 추가 및 삭제 명령을 처리하는 컨트롤러입니다.
 * 모든 요청은 AuthInterceptor를 통해 requestingUserId가 검증됩니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
public class UserRoomMembershipCommandController {

    private final UserRoomMembershipCommandService userRoomMembershipCommandService;

    /**
     * 1. room에 user 추가 API: POST /room/{roomId}/user
     * * @param requestingUserId JWT에서 추출된 요청자 ID
     * @param roomId 멤버를 추가할 방의 ID
     * @param request 추가할 사용자의 이메일 및 역할 정보
     * @return 추가된 멤버 정보 (RoomMemberDto)
     */
    @PostMapping("/{roomId}/user")
    public ResponseEntity<RoomMemberResponse> addMemberToRoom(
            @AuthorizedUser Long requestingUserId,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomMemberAddRequest request
    ) {
        RoomMemberResponse response = userRoomMembershipCommandService.addMemberToRoom(
                requestingUserId,
                roomId,
                request
        );
        // 명세서에 따라 201 Created 응답
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. room에서 user 삭제 API: DELETE /room/{roomId}/user/{userId}
     * * @param requestingUserId JWT에서 추출된 요청자 ID
     * @param roomId 멤버를 삭제할 방의 ID
     * @param userIdForDeletion 삭제 대상이 되는 사용자의 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{roomId}/user/{userIdForDeletion}")
    public ResponseEntity<Void> removeMemberFromRoom(
            @AuthorizedUser Long requestingUserId,
            @PathVariable Long roomId,
            @PathVariable(name = "userIdForDeletion") Long userIdForDeletion
    ) {
        userRoomMembershipCommandService.removeMemberFromRoom(
                requestingUserId,
                roomId,
                userIdForDeletion
        );
        // 명세서에 따라 204 No Content 응답
        return ResponseEntity.noContent().build();
    }
}
