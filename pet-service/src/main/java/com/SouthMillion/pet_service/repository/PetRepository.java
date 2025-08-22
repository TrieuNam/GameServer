package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.entity.PetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<PetEntity, Long> {
    List<PetEntity> findByRoleId(String roleId);
    Optional<PetEntity> findByRoleIdAndPetIndex(String roleId, Integer petIndex);
}