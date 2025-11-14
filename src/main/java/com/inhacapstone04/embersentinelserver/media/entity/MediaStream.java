package com.inhacapstone04.embersentinelserver.media.entity;

import com.inhacapstone04.embersentinelserver.common.entity.CreatedAtEntity;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "media_stream")
@Entity
@Getter
@Setter
public class MediaStream extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "livekit_room_name")
    private String livekitRoomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "streaming_status")
    private StreamingStatus streamingStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fire_event_id", nullable = false)
    private FireEvent fireEvent;
}
