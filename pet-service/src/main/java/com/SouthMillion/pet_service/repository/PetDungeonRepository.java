package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.model.entity.PetDungeon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetDungeonRepository extends JpaRepository<PetDungeon, Long> {
    Optional<PetDungeon> findByUserId(String userId);
}
