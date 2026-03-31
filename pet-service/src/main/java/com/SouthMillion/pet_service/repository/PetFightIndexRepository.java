package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.model.entity.PetFightIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetFightIndexRepository extends JpaRepository<PetFightIndex, Long> {

    /**
     * Find fight index configuration for a user
     */
    Optional<PetFightIndex> findByUserId(String userId);
}
