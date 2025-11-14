package com.inhacapstone04.embersentinelserver.room.entity;

import com.inhacapstone04.embersentinelserver.common.entity.CreatedAtEntity;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "user_room_membership", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "room_id"})
})
@Entity
@Getter
@Setter
public class UserRoomMembership extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // (3) FK: user_id
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false) // (4) FK: room_id
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MembershipRole role;

    protected UserRoomMembership() {}

    public UserRoomMembership(User user, Room room) {
        this.user = user;
        this.room = room;
    }
}
