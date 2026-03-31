package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.StarMapGala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StarMapGalaRepository extends JpaRepository<StarMapGala, Long> {
    Optional<StarMapGala> findByRoleId(Long roleId);
}
