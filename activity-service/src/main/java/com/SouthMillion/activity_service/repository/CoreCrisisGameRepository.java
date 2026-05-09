package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.CoreCrisisGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CoreCrisisGameRepository extends JpaRepository<CoreCrisisGame, Long> {
    Optional<CoreCrisisGame> findByRoleId(Long roleId);

    default Optional<CoreCrisisGame> safeFindByRoleId(Long roleId) {
        if (roleId == null || roleId < 1 || roleId > 10000) { // Assuming 10000 is the upper limit
            throw new IllegalArgumentException("Invalid roleId: must be between 1 and 10000.");
        }
        return findByRoleId(roleId);
    }
}