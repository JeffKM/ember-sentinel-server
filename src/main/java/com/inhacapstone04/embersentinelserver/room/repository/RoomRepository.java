package com.inhacapstone04.embersentinelserver.room.repository;

import com.inhacapstone04.embersentinelserver.room.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
