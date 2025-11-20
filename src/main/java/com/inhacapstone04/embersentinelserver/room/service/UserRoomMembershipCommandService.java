package com.inhacapstone04.embersentinelserver.room.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.dto.RoomMemberDTO;
import com.inhacapstone04.embersentinelserver.room.dto.request.RoomMemberAddRequest;
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
public class UserRoomMembershipCommandService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final UserRoomMembershipRepository membershipRepository;

    private static final MembershipRole REQUIRED_ROLE_TO_ADD = MembershipRole.EDITOR;

    /**
     * Room에 사용자를 추가합니다. (권한, 존재, 중복 검증 포함)
     *
     * @param requestingUserId 멤버를 추가하려는 요청자의 ID
     * @param roomId 멤버를 추가할 방의 ID
     * @param request 추가할 사용자의 이메일과 역할 정보
     * @return 추가된 멤버의 정보 (RoomMemberDto)
     */
    @Transactional
    public RoomMemberDTO addMemberToRoom(
            Long requestingUserId,
            Long roomId,
            RoomMemberAddRequest request
    ) {
        // 1. [권한 검증] 요청자가 해당 Room의 OWNER(명세서 기준) 또는 EDITOR 권한 이상인지 확인
        validateRequesterPermission(requestingUserId, roomId);

        // 2. [사용자 존재 검증] 추가할 대상(targetUser)의 이메일로 User 정보를 조회 (404 Not Found)
        User targetUser = userRepository.findByEmail(request.userEmail())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND_BY_EMAIL,
                        request.userEmail() + "에 해당하는 사용자를 찾을 수 없습니다."
                ));

        // 3. [중복 검증] 대상 사용자가 이미 해당 Room의 멤버인지 확인 (409 Conflict)
        if (membershipRepository.existsByUser_IdAndRoom_Id(targetUser.getId(), roomId)) {
            throw new CustomException(
                    ErrorCode.ALREADY_MEMBER_OF_ROOM,
                    "사용자 (" + request.userEmail() + ")는 이미 Room ID " + roomId + "의 멤버입니다."
            );
        }

        // 4. [Room 엔티티 조회] 멤버십 생성을 위해 Room 엔티티를 가져옵니다. (RoomNotFound는 1번에서 간접적으로 처리되지만 명시적으로 가져옴)
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID));

        // 5. [멤버십 생성 및 저장]
        UserRoomMembership newMembership = new UserRoomMembership(targetUser, room);

        // 요청된 역할(MemberRole)을 MembershipRole로 변환하여 설정합니다.
        // 참고: 명세서의 MemberRole은 Enum으로 존재하지 않으므로, MembershipRole을 사용한다고 가정
        newMembership.setRole(MembershipRole.valueOf(request.role().name()));

        membershipRepository.save(newMembership);

        // 6. DTO 변환 및 반환
        return RoomMemberDTO.of(targetUser, newMembership.getRole());
    }

    /**
     * 요청자(requester)가 해당 Room에 멤버를 추가할 권한(EDITOR 이상)을 가졌는지 검증합니다.
     */
    private void validateRequesterPermission(Long requestingUserId, Long roomId) {
        // 1. 요청자의 멤버십 조회
        UserRoomMembership membership = membershipRepository
                .findByUser_IdAndRoom_Id(requestingUserId, roomId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID,
                        "Room ID " + roomId + "에 대한 멤버십이 없어 권한이 없습니다."
                ));

        // 2. 권한 레벨 확인: 요청자가 EDITOR 권한 이상인지 확인
        // EDITOR 역할이 요구되는 경우, 요청자의 역할이 EDITOR 또는 그 이상(ADMIN)인지 확인
        if (membership.getRole().ordinal() < REQUIRED_ROLE_TO_ADD.ordinal()) {
            throw new CustomException(
                    ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID,
                    "멤버를 추가하려면 최소 " + REQUIRED_ROLE_TO_ADD.name() + " 권한이 필요합니다. 현재 권한: " + membership.getRole().name()
            );
        }
    }
}
