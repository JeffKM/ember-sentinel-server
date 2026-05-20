package com.inhacapstone04.embersentinelserver.support;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.fire_event.entity.DetectionType;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import com.inhacapstone04.embersentinelserver.room.entity.MembershipRole;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.entity.UserRoomMembership;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import com.inhacapstone04.embersentinelserver.room.repository.UserRoomMembershipRepository;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.UserRole;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트 베이스 클래스
 * - PostgreSQL: jdbc:tc: URL로 Testcontainers 자동 관리
 * - Redis: 싱글턴 컨테이너 (Spring 컨텍스트 캐싱과 호환)
 */
@SpringBootTest
public abstract class IntegrationTestSupport {

    static final GenericContainer<?> redisContainer;

    static {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redisContainer.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    // 공통 Repository 주입
    @Autowired protected UserRepository userRepository;
    @Autowired protected BuildingRepository buildingRepository;
    @Autowired protected RoomRepository roomRepository;
    @Autowired protected UserRoomMembershipRepository membershipRepository;
    @Autowired protected CameraEdgeRepository cameraEdgeRepository;
    @Autowired protected FireEventRepository fireEventRepository;
    @Autowired protected MediaStreamRepository mediaStreamRepository;

    // --- 공통 헬퍼 메서드 ---

    protected User createUser(String email, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setUserRole(UserRole.USER);
        user.setAuthType(AuthType.GOOGLE);
        return userRepository.save(user);
    }

    protected Building createBuilding(String name) {
        Building building = new Building();
        building.setBuildingName(name);
        return buildingRepository.save(building);
    }

    protected Room createRoom(String alias, String floor, String roomNum, Building building) {
        Room room = new Room();
        room.setRoomAlias(alias);
        room.setBuildingLocationFloor(floor);
        room.setRoomNumber(roomNum);
        room.setBuilding(building);
        return roomRepository.save(room);
    }

    protected UserRoomMembership createMembership(User user, Room room, MembershipRole role) {
        UserRoomMembership membership = new UserRoomMembership(user, room);
        membership.setRole(role);
        room.getUserMemberships().add(membership);
        return membershipRepository.save(membership);
    }

    protected CameraEdge createCamera(Room room, String uuid, String alias) {
        CameraEdge camera = new CameraEdge();
        camera.setRoom(room);
        camera.setDeviceUuid(uuid);
        camera.setCameraEdgeAlias(alias);
        room.getCameraEdges().add(camera);
        return cameraEdgeRepository.save(camera);
    }

    protected FireEvent createFireEvent(CameraEdge camera) {
        FireEvent event = new FireEvent();
        event.setCameraEdge(camera);
        event.setDetectionType(DetectionType.FIRE);
        return fireEventRepository.save(event);
    }

    protected MediaStream createMediaStream(FireEvent event, StreamingStatus status) {
        MediaStream stream = new MediaStream();
        stream.setFireEvent(event);
        stream.setStreamingStatus(status);
        return mediaStreamRepository.save(stream);
    }
}
