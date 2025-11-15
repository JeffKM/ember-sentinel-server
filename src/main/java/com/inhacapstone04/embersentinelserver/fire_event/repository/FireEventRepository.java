package com.inhacapstone04.embersentinelserver.fire_event.repository;

import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FireEventRepository extends JpaRepository<FireEvent, Long> {
}
