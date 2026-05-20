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

import java.util.UUID;

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

        // 4. [카메라 생성 및 저장] — API Key 자동 생성
        String apiKey = UUID.randomUUID().toString();
        CameraEdge newCamera = new CameraEdge(
                room,
                request.deviceUuid(),
                request.cameraEdgeAlias(),
                apiKey
        );

        // 5. DTO 반환
        return CameraEdgeResponse.of(cameraEdgeRepository.save(newCamera));
    }

    /**
     * Room에서 특정 Camera Edge를 삭제합니다.
     *
     * @param requestingUserId 요청자 ID
     * @param roomId 카메라가 속한 Room ID
     * @param cameraEdgeId 삭제할 카메라 ID
     */
    @Transactional
    public void deleteCameraEdge(Long requestingUserId, Long roomId, Long cameraEdgeId) {

        // 1. [권한 검증] 요청자가 해당 Room의 EDITOR 권한 이상인지 확인 (403 Forbidden)
        userRoomMembershipCommandService.validateRequesterPermission(requestingUserId, roomId, REQUIRED_ROLE);

        // 2. [카메라 존재 확인] ID로 조회 (404 Not Found)
        CameraEdge cameraEdge = cameraEdgeRepository.findById(cameraEdgeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID));

        // 3. [소속 확인] 해당 카메라가 요청된 Room에 속해 있는지 확인 (404 Not Found)
        // 보안상 다른 방의 카메라 존재 여부를 노출하지 않기 위해 roomId 불일치 시에도 Not Found 처리
        if (!cameraEdge.getRoom().getId().equals(roomId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_BY_ID);
        }

        // 4. [삭제]
        // JPA의 Cascade 설정에 따라 연관된 FireEvent 등도 함께 삭제될 수 있습니다.
        cameraEdgeRepository.delete(cameraEdge);
    }
}
