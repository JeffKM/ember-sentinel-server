package com.inhacapstone04.embersentinelserver.fire_event.service;

import com.inhacapstone04.embersentinelserver.camera_edge.dto.CameraEdgeDTO;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.fire_event.dto.FireEventSimpleDTO;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventDetailResponse;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.dto.MediaRecordDTO;
import com.inhacapstone04.embersentinelserver.media.dto.MediaStreamDTO;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FireEventQueryService {

    private final FireEventRepository fireEventRepository;
    private final UserRoomMembershipRepository membershipRepository;
    private final CameraEdgeRepository cameraEdgeRepository;

    /**
     * 특정 화재 탐지 이벤트의 상세 정보를 조회합니다.
     *
     * @param requestingUserId 요청자 ID
     * @param roomId 방 ID
     * @param eventId 이벤트 ID
     * @return FireEventDetailDTO
     */
    public FireEventDetailResponse getFireEventDetail(Long requestingUserId, Long roomId, Long eventId) {

        // 1. [권한 검증] 요청자가 해당 Room의 멤버인지 확인 (403 Forbidden)
        if (!membershipRepository.existsByUser_IdAndRoom_Id(requestingUserId, roomId)) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID, "해당 Room에 접근 권한이 없습니다.");
        }

        // 2. [이벤트 조회] fire_event 테이블에서 조회 (404 Not Found)
        // JOIN FETCH를 사용하여 N+1 문제를 방지하고 연관 엔티티(CameraEdge, MediaStream)를 한 번에 가져오는 것이 좋습니다.
        // (Repository에 해당 쿼리 메서드가 없다면 Lazy Loading으로 동작합니다.)
        FireEvent fireEvent = fireEventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "요청한 화재 이벤트를 찾을 수 없습니다."));

        // 3. [소속 확인] 조회된 이벤트의 카메라가 요청된 Room에 속하는지 확인 (404 Not Found)
        CameraEdge cameraEdge = fireEvent.getCameraEdge();
        if (cameraEdge == null || !cameraEdge.getRoom().getId().equals(roomId)) {
            // 보안상, 다른 방의 이벤트 ID를 무작위로 대입해 보는 것을 방지하기 위해 Not Found로 처리
            throw new CustomException(ErrorCode.NOT_FOUND_BY_ID, "해당 방에 속하지 않는 이벤트입니다.");
        }

        // 4. DTO 조립
        CameraEdgeDTO cameraInfo = CameraEdgeDTO.of(cameraEdge);
        MediaStreamDTO streamInfo = MediaStreamDTO.of(fireEvent.getMediaStream());
        MediaRecordDTO mediaInfo = MediaRecordDTO.of(fireEvent.getMediaRecord());

        return FireEventDetailResponse.of(fireEvent, cameraInfo, streamInfo, mediaInfo);
    }

    /**
     * 특정 카메라가 탐지한 과거 화재 이벤트 목록을 조회합니다.
     *
     * @param requestingUserId 요청자 ID
     * @param roomId 방 ID
     * @param cameraEdgeId 카메라 ID
     * @param pageable 페이징 정보
     * @return PageResponse<FireEventSimpleDTO>
     */
    public PageResponse<FireEventSimpleDTO> getFireEventList(Long requestingUserId, Long roomId, Long cameraEdgeId, Pageable pageable) {

        // 1. [권한 검증] 요청자가 해당 Room의 멤버인지 확인 (403 Forbidden)
        if (!membershipRepository.existsByUser_IdAndRoom_Id(requestingUserId, roomId)) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID, "해당 Room에 접근 권한이 없습니다.");
        }

        // 2. [카메라 검증] 카메라가 존재하고, 해당 Room에 속해있는지 확인 (404 Not Found)
        CameraEdge cameraEdge = cameraEdgeRepository.findById(cameraEdgeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "요청한 카메라를 찾을 수 없습니다."));

        if (!cameraEdge.getRoom().getId().equals(roomId)) {
            // 보안상 404 Not Found 반환 (다른 방의 카메라 존재 여부 숨김)
            throw new CustomException(ErrorCode.NOT_FOUND_BY_ID, "해당 방에 속하지 않는 카메라입니다.");
        }

        // 3. [이벤트 조회] 해당 카메라의 화재 이벤트 페이징 조회
        Page<FireEvent> eventPage = fireEventRepository.findAllByCameraEdge_Id(cameraEdgeId, pageable);

        // 4. [DTO 변환]
        return PageResponse.of(eventPage.map(FireEventSimpleDTO::of));
    }
}
