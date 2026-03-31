package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.model.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Pet.PetId> {

    /**
     * Find all pets for a user
     */
    List<Pet> findByUserId(String userId);

    /**
     * Find a specific pet by user ID and pet index
     */
    Optional<Pet> findByUserIdAndPetIndex(String userId, Integer petIndex);

    /**
     * Count pets for a user
     */
    long countByUserId(String userId);

    /**
     * Find pets by user ID and pet type
     */
    List<Pet> findByUserIdAndPetId(String userId, Integer petId);

    /**
     * Find highest pet index for a user
     */
    @Query("SELECT COALESCE(MAX(p.petIndex), 0) FROM Pet p WHERE p.userId = :userId")
    Integer findMaxPetIndexByUserId(@Param("userId") String userId);

    /**
     * Delete a pet by user ID and pet index
     */
    void deleteByUserIdAndPetIndex(String userId, Integer petIndex);

    /**
     * Check if pet exists
     */
    boolean existsByUserIdAndPetIndex(String userId, Integer petIndex);
}
