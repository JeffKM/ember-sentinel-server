package com.inhacapstone04.embersentinelserver.media.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.dto.response.MediaRecordResponse;
import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaRecordQueryService {

    private final FireEventRepository fireEventRepository;
    private final UserRoomMembershipRepository membershipRepository;

    /**
     * 특정 화재 이벤트의 녹화 파일 정보를 조회합니다.
     *
     * @param userId      요청자 ID
     * @param roomId      방 ID (권한 확인용)
     * @param fireEventId 화재 이벤트 ID
     * @return 녹화 파일 정보
     */
    public MediaRecordResponse getMediaRecord(Long userId, Long roomId, Long fireEventId) {
        // 1. [권한 검증] 요청자가 해당 Room의 멤버인지 확인 (403 Forbidden)
        if (!membershipRepository.existsByUser_IdAndRoom_Id(userId, roomId)) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID, "해당 Room에 접근 권한이 없습니다.");
        }

        // 2. [이벤트 조회] FireEvent 조회 (404 Not Found)
        FireEvent fireEvent = fireEventRepository.findById(fireEventId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "요청한 화재 이벤트를 찾을 수 없습니다."));

        // 3. [소속 확인] 이벤트가 요청된 방에 속해 있는지 검증 (404 Not Found)
        if (!fireEvent.getCameraEdge().getRoom().getId().equals(roomId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_BY_ID, "해당 방에 속하지 않는 이벤트입니다.");
        }

        // 4. [녹화 파일 조회]
        MediaRecord mediaRecord = fireEvent.getMediaRecord();
        if (mediaRecord == null) {
            throw new CustomException(ErrorCode.NOT_FOUND_BY_ID, "해당 이벤트에 대한 녹화 파일이 존재하지 않습니다.");
        }

        return MediaRecordResponse.of(mediaRecord);
    }
}
