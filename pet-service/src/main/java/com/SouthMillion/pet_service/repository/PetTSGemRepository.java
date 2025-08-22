package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.entity.PetTSGemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetTSGemRepository extends JpaRepository<PetTSGemEntity, Long> {
    List<PetTSGemEntity> findByRoleId(String roleId);
    Optional<PetTSGemEntity> findByRoleIdAndGemIndex(String roleId, Integer gemIndex);
}