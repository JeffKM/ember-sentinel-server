package com.inhacapstone04.embersentinelserver.fire_event.entity;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.common.entity.CreatedAtEntity;
import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "fire_event")
@Entity
@Getter
@Setter
public class FireEvent extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_type", nullable = false) // [추가됨] 감지 유형 (FIRE/SMOKE)
    private DetectionType detectionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fire_cause", nullable = true)
    private FireCause fireCause;

    @Column(name = "risk_rank", nullable = true)
    private Long riskRank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_edge_id", nullable = false)
    private CameraEdge cameraEdge;

    @OneToOne(mappedBy = "fireEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private MediaStream mediaStream;

    @OneToOne(mappedBy = "fireEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private MediaRecord mediaRecord;
}
