package com.inhacapstone04.embersentinelserver.media.repository;

import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaStreamRepository extends JpaRepository<MediaStream, Long> {
}
