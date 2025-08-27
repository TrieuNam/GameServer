package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.entity.PetRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRoleRepository extends JpaRepository<PetRoleEntity, Long> {
    List<PetRoleEntity> findAllByRoleIdOrderByPetIndexAsc(String roleId);

    Optional<PetRoleEntity> findByRoleIdAndPetIndex(String roleId, Integer petIndex);
}