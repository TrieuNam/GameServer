package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.entity.PetFightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetFightRepository extends JpaRepository<PetFightEntity, Long> {
    Optional<PetFightEntity> findByRoleId(String roleId);
}