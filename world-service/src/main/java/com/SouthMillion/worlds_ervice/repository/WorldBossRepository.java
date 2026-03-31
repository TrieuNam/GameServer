package com.SouthMillion.worlds_ervice.repository;

import com.SouthMillion.worlds_ervice.entity.WorldBoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorldBossRepository extends JpaRepository<WorldBoss, Long> {
    
    List<WorldBoss> findByStatus(WorldBoss.BossStatus status);
    
    Optional<WorldBoss> findFirstByStatusOrderBySpawnTimeDesc(WorldBoss.BossStatus status);
    
    List<WorldBoss> findTop10ByOrderBySpawnTimeDesc();
}
