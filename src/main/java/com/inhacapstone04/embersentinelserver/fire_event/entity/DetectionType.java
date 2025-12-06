package com.inhacapstone04.embersentinelserver.fire_event.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DetectionType {
    FIRE("화재"),
    SMOKE("연기");

    private final String description;
}
