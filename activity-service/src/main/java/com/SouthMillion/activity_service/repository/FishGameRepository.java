package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.FishGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FishGameRepository extends JpaRepository<FishGame, Long> {
    Optional<FishGame> findByRoleId(Long roleId);
}
