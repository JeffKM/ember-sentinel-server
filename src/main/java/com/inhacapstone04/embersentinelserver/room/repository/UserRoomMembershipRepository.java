package com.inhacapstone04.embersentinelserver.room.repository;

import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoomMembershipRepository extends JpaRepository<UserRoomMembership, Long> {
    /**
     * 유저가 요청한 Room ID 리스트 중,
     * 실제로 해당 유저가 멤버십을 가진 Room의 개수를 카운트합니다. (검증용)
     *
     * @param userId (검증할 유저 ID)
     * @param roomIds (유저가 요청한 Room ID 리스트)
     * @return 유저가 권한을 가진 Room의 개수
     */
    @Query(value = "SELECT COUNT(DISTINCT m.room.id) " +
            "FROM UserRoomMembership m " +
            "WHERE m.user.id = :userId " +
            "AND m.room.id IN :roomIds")
    long countValidRoomsForUser(@Param("userId") Long userId, @Param("roomIds") List<Long> roomIds);

    /**
     * userId와 roomId로 멤버십이 존재하는지 확인 (권한 검증용)
     * Spring Data JPA가 메서드 이름을 분석하여 쿼리를 생성합니다.
     *
     * @return (존재하면 true, 없으면 false)
     */
    boolean existsByUser_IdAndRoom_Id(Long userId, Long roomId);

    Optional<UserRoomMembership> findByUser_IdAndRoom_Id(Long requestingUserId, Long roomId);

    // 특정 Room의 멤버들 중 FCM 토큰이 있는 유저들의 토큰 목록 조회
    @Query("SELECT u.fcmToken FROM UserRoomMembership urm JOIN urm.user u " +
            "WHERE urm.room.id = :roomId AND u.fcmToken IS NOT NULL AND u.fcmToken <> ''")
    List<String> findAllFcmTokensByRoomId(@Param("roomId") Long roomId);
}
