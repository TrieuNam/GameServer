package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.LoopMine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LoopMineRepository extends JpaRepository<LoopMine, Long> {
    Optional<LoopMine> findByRoleId(Long roleId);
    
    // New method for paginated battle log retrieval
    Page<LoopMine> findByRoleId(Long roleId, Pageable pageable);
}