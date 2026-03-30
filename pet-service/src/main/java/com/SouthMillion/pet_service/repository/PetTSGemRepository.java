package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.model.entity.PetTSGem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetTSGemRepository extends JpaRepository<PetTSGem, PetTSGem.PetTSGemId> {

    /**
     * Find all special gems for a user
     */
    List<PetTSGem> findByUserId(String userId);

    /**
     * Find a specific special gem
     */
    Optional<PetTSGem> findByUserIdAndGemIndex(String userId, Integer gemIndex);

    /**
     * Find special gems equipped on a specific pet
     */
    List<PetTSGem> findByUserIdAndPetIndex(String userId, Integer petIndex);

    /**
     * Find unequipped special gems (in bag)
     */
    List<PetTSGem> findByUserIdAndPetIndex(String userId, int petIndex);

    /**
     * Count special gems for a user
     */
    long countByUserId(String userId);

    /**
     * Find highest gem index for a user
     */
    @Query("SELECT COALESCE(MAX(g.gemIndex), 0) FROM PetTSGem g WHERE g.userId = :userId")
    Integer findMaxGemIndexByUserId(@Param("userId") String userId);

    /**
     * Delete a special gem
     */
    void deleteByUserIdAndGemIndex(String userId, Integer gemIndex);
    
    /**
     * Find gems by user, exact level, and pet index (for same-level material finding)
     */
    List<PetTSGem> findByUserIdAndGemLevelAndPetIndex(String userId, Integer gemLevel, Integer petIndex);

    /**
     * Find gems by level range
     */
    List<PetTSGem> findByUserIdAndGemLevelLessThanEqualAndPetIndex(
        String userId, Integer maxLevel, Integer petIndex);
}
