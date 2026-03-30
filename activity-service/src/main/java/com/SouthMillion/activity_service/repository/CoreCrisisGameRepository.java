package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.CoreCrisisGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CoreCrisisGameRepository extends JpaRepository<CoreCrisisGame, Long> {
    Optional<CoreCrisisGame> findByRoleId(Long roleId);
}
