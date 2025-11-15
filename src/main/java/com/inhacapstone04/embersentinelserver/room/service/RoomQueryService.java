package com.inhacapstone04.embersentinelserver.room.service;

import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.room.dto.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // CQS 원칙에 따라 조회 전용 서비스는 readOnly=true
public class RoomQueryService {

    private final RoomRepository roomRepository;

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
}
