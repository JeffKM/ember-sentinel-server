package com.inhacapstone04.embersentinelserver.building.repository;


import com.inhacapstone04.embersentinelserver.building.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {
}
