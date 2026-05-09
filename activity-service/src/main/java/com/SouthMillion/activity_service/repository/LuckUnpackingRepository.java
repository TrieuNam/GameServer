package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.LuckUnpacking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LuckUnpackingRepository extends JpaRepository<LuckUnpacking, Long> {
    
    // Existing single lookup
    LuckUnpacking findByRoleId(Long roleId);
    
    // Batch operation for N+1 fix
    List<LuckUnpacking> findByRoleIdIn(List<Long> roleIds);
}