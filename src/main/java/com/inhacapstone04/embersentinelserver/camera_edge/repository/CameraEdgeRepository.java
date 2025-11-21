package com.inhacapstone04.embersentinelserver.camera_edge.repository;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CameraEdgeRepository extends JpaRepository<CameraEdge,Long> {
    boolean existsByDeviceUuid(String deviceUuid);

    Optional<CameraEdge> findByDeviceUuid(@NotBlank String deviceUuid);
}
