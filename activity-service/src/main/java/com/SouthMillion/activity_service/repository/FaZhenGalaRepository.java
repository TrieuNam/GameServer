package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.FaZhenGala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaZhenGalaRepository extends JpaRepository<FaZhenGala, Long> {
    Optional<FaZhenGala> findByRoleId(Long roleId);
}
