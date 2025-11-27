package com.inhacapstone04.embersentinelserver.room.repository;

import com.inhacapstone04.embersentinelserver.camera_edge.dto.CameraEdgeWithIsFireDTO;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.room.dto.RoomStatisticsDTO;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * 모든 Room 엔티티를 페이징 처리하여 조회합니다.
     *
     * "JOIN FETCH r.building"
     * N+1 문제를 피하기 위해, Room을 조회할 때 연관된 Building 엔티티도
     * 즉시 함께 로드(Eager Loading)합니다.
     *
     * "COUNT(r)"
     * Paging을 위한 count 쿼리는 별도로 효율적으로 실행합니다.
     */
    @Query(value = "SELECT r FROM Room r JOIN FETCH r.building",
            countQuery = "SELECT COUNT(r) FROM Room r")
    Page<Room> findAllWithBuilding(Pageable pageable);

    /**
     * 특정 유저 ID와 연관된 Room 목록을 페이징하여 조회합니다.
     * UserRoomMembership(m)을 통해 유저(m.user.id)를 필터링합니다.
     *
     * "JOIN FETCH r.building b" : N+1 방지를 위해 Building도 함께 조회
     * "SELECT DISTINCT r" : JOIN으로 인한 Room 중복 제거
     * "countQuery" : 페이징을 위한 별도 count 쿼리
     */
    @Query(value = "SELECT DISTINCT r FROM Room r " +
            "JOIN FETCH r.building b " +
            "JOIN r.userMemberships m " +
            "WHERE m.user.id = :userId",
            countQuery = "SELECT COUNT(DISTINCT r) FROM Room r " +
                    "JOIN r.userMemberships m " +
                    "WHERE m.user.id = :userId")
    Page<Room> findRoomsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * roomId 목록을 기반으로 Room 통계 정보를 조회합니다.
     * N+1 문제를 해결하기 위해 DTO로 직접 조회합니다.
     *
     * @param roomIds 조회할 Room의 ID 목록
     * @return List<RoomStatisticsDto>
     */
    @Query(value = "SELECT NEW com.inhacapstone04.embersentinelserver.room.dto.RoomStatisticsDTO(" +
            "   r.id, " +
            "   r.roomAlias, " +
            "   b.buildingName, " +
            "   r.buildingLocationFloor, " +
            "   r.roomNumber, " +
            "   COUNT(DISTINCT c.id), " +
            "   COUNT(DISTINCT ms.id) " +
            ") " +
            "FROM Room r " +
            "JOIN r.building b " +
            "LEFT JOIN r.cameraEdges c " +
            "LEFT JOIN c.fireEvents f " +
            "LEFT JOIN f.mediaStream ms " +
            "   WITH ms.streamingStatus = :status " +
            "WHERE r.id IN :roomIds " +
            "GROUP BY r.id, r.roomAlias, b.buildingName, r.buildingLocationFloor, r.roomNumber")
    List<RoomStatisticsDTO> findRoomStatisticsByIds(@Param("roomIds") List<Long> roomIds,
                                                    @Param("status") StreamingStatus status);

    /**
     * Room 상세 조회 (N+1 방지)
     * roomId로 Room을 조회할 때, 연관된 Building,
     * UserRoomMemberships, 그리고 각 Membership의 User까지
     * 모두 JOIN FETCH하여 한 번의 쿼리로 가져옵니다.
     */
    @Query("SELECT r FROM Room r " +
            "JOIN FETCH r.building b " +
            "LEFT JOIN FETCH r.userMemberships m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE r.id = :roomId")
    Optional<Room> findRoomDetailsById(@Param("roomId") Long roomId);


    /**
     * 특정 Room의 카메라 목록 및 라이브 화재 정보 조회
     * CameraEdge(c)를 기준으로 FireEvent(f), MediaStream(ms)을 LEFT JOIN합니다.
     * 'LIVE' 상태인 화재 정보가 없더라도 카메라 목록은 조회되어야 합니다.
     * CameraEdgeWithIsFireDTO의 생성자 시그니처에 맞춰 필드 추가
     * (c.id, c.deviceUuid, c.cameraEdgeAlias, c.room.buildingLocationFloor, c.room.roomNumber, f.id, ms.streamingStatus)
     */
    @Query("SELECT NEW com.inhacapstone04.embersentinelserver.camera_edge.dto.CameraEdgeWithIsFireDTO(" +
            "   c.id, " +
            "   c.deviceUuid, " +
            "   c.cameraEdgeAlias, " +
            "   c.room.buildingLocationFloor, " + // 추가됨
            "   c.room.roomNumber, " +            // 추가됨
            "   f.id, " +
            "   ms.streamingStatus" +
            ") " +
            "FROM CameraEdge c " +
            "LEFT JOIN c.fireEvents f " +
            "LEFT JOIN f.mediaStream ms " +
            "   WITH ms.streamingStatus = :status " +
            "WHERE c.room.id = :roomId " +
            "ORDER BY c.id ASC")
    List<CameraEdgeWithIsFireDTO> findCameraDetailsByRoomId(@Param("roomId") Long roomId, @Param("status") StreamingStatus status);
}
