package com.inhacapstone04.embersentinelserver.room.repository;

import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
