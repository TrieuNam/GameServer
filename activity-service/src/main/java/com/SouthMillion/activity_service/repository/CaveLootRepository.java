package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.CaveLoot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaveLootRepository extends JpaRepository<CaveLoot, Long> {
    Optional<CaveLoot> findByRoleId(Long roleId);
}
