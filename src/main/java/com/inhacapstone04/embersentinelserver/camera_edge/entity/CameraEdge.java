package com.inhacapstone04.embersentinelserver.camera_edge.entity;

import com.inhacapstone04.embersentinelserver.common.entity.BaseEntity;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Table(name = "camera_edge")
@Entity
@Getter
@Setter
public class CameraEdge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_uuid")
    private String deviceUuid;

    @Column(name = "camera_edge_alias")
    private String cameraEdgeAlias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "cameraEdge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FireEvent> fireEvents = new ArrayList<>();
}
