package com.inhacapstone04.embersentinelserver.room.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.room.dto.RoomDashboardResponse;
import com.inhacapstone04.embersentinelserver.room.dto.RoomStatisticsDTO;
import com.inhacapstone04.embersentinelserver.room.dto.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // CQS 원칙에 따라 조회 전용 서비스는 readOnly=true
public class RoomQueryService {

    private final RoomRepository roomRepository;
    private final UserRoomMembershipRepository userRoomMembershipRepository;

    /**
     * 현재 로그인된 유저(@AuthorizedUser)와 연관된 Room의 정보를 페이징하여 조회합니다.
     *
     * @param userId @AuthorizedUser로 주입된 유저 ID
     * @param pageable (페이지 번호, 페이지 크기, 정렬)
     * @return PageResponse<SingleRoomResponse>
     */
    public PageResponse<SingleRoomResponse> getMyRooms(Long userId, Pageable pageable) {

        // 1. Repository에서 Page<Room>을 조회합니다.
        //    (userId로 필터링하며, 'building'이 JOIN FETCH 되어 있습니다.)
        Page<Room> roomPage = roomRepository.findRoomsByUserId(userId, pageable);

        // 2. Page<Room>을 Page<SingleRoomResponse>로 변환(map)합니다.
        //    room.getBuilding().getBuildingName() 호출 시 N+1 쿼리가 발생하지 않습니다.
        //    (Building 엔티티에 getBuildingName() 메서드가 있다고 가정)
        Page<SingleRoomResponse> responseDtoPage = roomPage.map(room ->
                SingleRoomResponse.of(room, room.getBuilding().getBuildingName())
        );

        // 3. Page<SingleRoomResponse>를 PageResponse로 감싸서 반환합니다.
        return PageResponse.of(responseDtoPage);
    }

    /**
     * [수정] Room ID 목록을 기반으로 대시보드 통계를 조회합니다.
     * (userId로 권한 검증 로직 추가)
     *
     * @param userId (권한 검증을 위한 @AuthorizedUser ID)
     * @param roomIds (조회할 Room ID 리스트)
     * @return RoomDashboardResponse (총계 및 개별 통계 포함)
     */
    public RoomDashboardResponse getRoomStatistics(Long userId, List<Long> roomIds) {

        // 1. 요청한 리스트가 비어있으면, 빈 대시보드 반환
        if (roomIds == null || roomIds.isEmpty()) {
            return RoomDashboardResponse.of(Collections.emptyList());
        }

        // 2. [검증] 요청한 roomIds 중, 유저가 실제로 권한을 가진 방의 개수를 확인
        long validRoomCount = userRoomMembershipRepository.countValidRoomsForUser(userId, roomIds);

        // 3. [권한 예외] 요청한 방의 개수와, 유저가 권한을 가진 방의 개수가 다르면
        // (즉, 권한 없는 방을 1개라도 요청했다면)
        if (validRoomCount != roomIds.size()) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID, "요청한 Room 목록에 접근 권한이 없습니다.");
        }

        // 4. [검증 통과] 통계 쿼리 실행
        List<RoomStatisticsDTO> roomList = roomRepository.findRoomStatisticsByIds(roomIds, StreamingStatus.LIVE);

        // 5. RoomDashboardResponse로 집계하여 반환
        return RoomDashboardResponse.of(roomList);
    }
}
