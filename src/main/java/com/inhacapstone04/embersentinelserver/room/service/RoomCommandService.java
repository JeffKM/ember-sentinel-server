package com.inhacapstone04.embersentinelserver.room.service;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.dto.request.RoomCreateRequest;
import com.inhacapstone04.embersentinelserver.room.dto.response.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomCommandService {

    private final RoomRepository roomRepository;
    private final UserRoomMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository; // buildingId 검증용

    /**
     * [POST] /room
     * 새로운 Room을 생성하고, 요청한 유저를 'EDITOR'로 등록합니다.
     *
     * @param userId (JWT 토큰에서 추출한 유저 ID)
     * @param request (Room 생성 DTO)
     * @return RoomSimpleResponse (생성된 Room 정보)
     */
    public SingleRoomResponse createRoom(Long userId, RoomCreateRequest request) {

        // 1. [DB 조회] 요청한 유저 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. [DB 조회] 요청한 빌딩 ID 검증 (명세서: 404 Not Found)
        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID));

        // 3. Room 엔티티 생성 및 저장
        Room newRoom = new Room();
        newRoom.setRoomAlias(request.roomAlias());
        newRoom.setBuildingLocationFloor(request.floor());
        newRoom.setRoomNumber(request.roomNumber());
        newRoom.setBuilding(building); // 연관관계 설정

        // [DB 저장 1] Room 저장 (DB에 INSERT되어 roomId가 생성됨)
        Room savedRoom = roomRepository.save(newRoom);

        // 4. UserRoomMembership 엔티티 생성
        UserRoomMembership membership = new UserRoomMembership(user, savedRoom);
        membership.setRole(MembershipRole.EDITOR); // EDITOR로 설정
        savedRoom.getUserMemberships().add(membership); // 양방향 연관관계 설정 (1차 캐시 일관성 유지)

        // [DB 저장 2] Membership 저장 (두 저장이 하나의 트랜잭션으로 묶임)
        membershipRepository.save(membership);

        // 5. 응답 DTO 반환
        return SingleRoomResponse.of(savedRoom, building.getBuildingName());
    }
}
