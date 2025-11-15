package com.inhacapstone04.embersentinelserver.room.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.room.dto.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
public class RoomQueryController {

    private final RoomQueryService roomQueryService;

    /**
     * [GET] /room/list/me
     * 현재 로그인된 유저(@AuthorizedUser)와 연관된 Room의 목록을 페이징하여 조회합니다.
     *
     * @param userId (@AuthorizedUser 어노테이션을 통해 리졸버가 주입)
     * @param pageable (PageableArgumentResolver가 쿼리 파라미터를 조합해 주입)
     * @return PageResponse<SingleRoomResponse>
     */
    @GetMapping("/list/me")
    public ResponseEntity<PageResponse<SingleRoomResponse>> getMyRoomsWithDefault(
            @AuthorizedUser Long userId,
            @PageableDefault(page = 1, size = 10) Pageable pageable
            // page= 파라미터가 없으면 1
            // size= 파라미터가 없으면 5
            // page 번호는 1부터 시작
    ) {

        PageResponse<SingleRoomResponse> response = roomQueryService.getMyRooms(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
