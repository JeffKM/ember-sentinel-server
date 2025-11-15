package com.inhacapstone04.embersentinelserver.room.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.room.dto.RoomDashboardResponse;
import com.inhacapstone04.embersentinelserver.room.dto.RoomListSummaryRequest;
import com.inhacapstone04.embersentinelserver.room.dto.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.service.RoomQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * [추가] (POST) /room/list/me/summary
     * 사용자가 요청한 Room 목록에 대한 통계(카메라, 화재)를 조회합니다.
     * (요청한 Room 목록 전부에 대한 멤버십 권한이 있는지 검증합니다)
     *
     * @param userId (@AuthorizedUser로 주입된 유저 ID)
     * @param request (@RequestBody로 Room ID 리스트 DTO를 받음)
     * @return RoomDashboardResponse
     */
    @PostMapping("/list/me/summary")
    public ResponseEntity<RoomDashboardResponse> getMyRoomStatistics(
            @AuthorizedUser Long userId,
            @Valid @RequestBody RoomListSummaryRequest request
    ) {
        RoomDashboardResponse response = roomQueryService.getRoomStatistics(userId, request.roomIds());
        return ResponseEntity.ok(response);
    }
}
