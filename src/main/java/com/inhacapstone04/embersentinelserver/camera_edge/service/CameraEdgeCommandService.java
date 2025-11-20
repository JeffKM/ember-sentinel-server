package com.inhacapstone04.embersentinelserver.camera_edge.service;

import com.inhacapstone04.embersentinelserver.camera_edge.dto.request.CameraEdgeCreateRequest;
import com.inhacapstone04.embersentinelserver.camera_edge.dto.response.CameraEdgeResponse;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.service.UserRoomMembershipCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CameraEdgeCommandService {

    private final CameraEdgeRepository cameraEdgeRepository;
    private final RoomRepository roomRepository;
    private final UserRoomMembershipCommandService userRoomMembershipCommandService;

    // 카메라 등록/삭제에 필요한 최소 권한 (EDITOR)
    private static final MembershipRole REQUIRED_ROLE = MembershipRole.EDITOR;

    /**
     * Room에 새로운 Camera Edge를 등록합니다.
     *
     * @param requestingUserId 요청자 ID
     * @param roomId 등록할 Room ID
     * @param request 카메라 생성 요청 정보 (UUID, Alias)
     * @return 등록된 카메라 정보 DTO
     */
    @Transactional
    public CameraEdgeResponse createCameraEdge(Long requestingUserId, Long roomId, CameraEdgeCreateRequest request) {

        // 1. [권한 검증] 요청자가 해당 Room의 EDITOR 권한 이상인지 확인 (403 Forbidden)
        // 공통 로직 적용: REQUIRED_ROLE을 인자로 전달
        userRoomMembershipCommandService.validateRequesterPermission(requestingUserId, roomId, REQUIRED_ROLE);

        // 2. [Room 존재 확인] Room ID 유효성 검사 (404 Not Found)
        // (권한 검증에서 멤버십을 통해 Room 존재가 간접 확인되지만, 명시적으로 Room 엔티티를 가져와야 함)
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID));

        // 3. [중복 검증] Device UUID가 이미 존재하는지 확인 (409 Conflict)
        if (cameraEdgeRepository.existsByDeviceUuid(request.deviceUuid())) {
            throw new CustomException(
                    ErrorCode.DUPLICATE_DEVICE_UUID,
                    "Device UUID " + request.deviceUuid() + "는 이미 등록되어 있습니다."
            );
        }

        // 4. [카메라 생성 및 저장]
        CameraEdge newCamera = new CameraEdge(
                room,
                request.deviceUuid(),
                request.cameraEdgeAlias()
        );

        // 5. DTO 반환
        return CameraEdgeResponse.of(cameraEdgeRepository.save(newCamera));
    }
}
