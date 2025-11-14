package com.inhacapstone04.embersentinelserver.building.entity;

import com.inhacapstone04.embersentinelserver.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "building")
@Entity
@NoArgsConstructor
@Getter
@Setter
public class Building extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "building_name")
    private String buildingName;

    public Building(String buildingName) {
        this.buildingName = buildingName;
    }
}
