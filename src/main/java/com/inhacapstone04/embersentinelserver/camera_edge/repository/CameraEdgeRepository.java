package com.inhacapstone04.embersentinelserver.camera_edge.repository;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CameraEdgeRepository extends JpaRepository<CameraEdge,Long> {
}
