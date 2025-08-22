package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.entity.PetClothEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetClothRepository extends JpaRepository<PetClothEntity, Long> {
    List<PetClothEntity> findByRoleId(String roleId);
}