package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.model.entity.PetCloth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetClothRepository extends JpaRepository<PetCloth, PetCloth.PetClothId> {

    /**
     * Find all clothing for a user
     */
    List<PetCloth> findByUserId(String userId);

    /**
     * Find specific clothing
     */
    Optional<PetCloth> findByUserIdAndClothId(String userId, Integer clothId);

    /**
     * Find clothing equipped on a specific pet
     */
    Optional<PetCloth> findByUserIdAndPetIndex(String userId, Integer petIndex);

    /**
     * Find clothing by level
     */
    List<PetCloth> findByUserIdAndLevelGreaterThan(String userId, Integer level);

    /**
     * Count clothing for a user
     */
    long countByUserId(String userId);
}
