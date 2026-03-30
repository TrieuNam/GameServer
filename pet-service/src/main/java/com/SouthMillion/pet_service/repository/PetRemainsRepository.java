package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.model.entity.PetRemains;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRemainsRepository extends JpaRepository<PetRemains, PetRemains.PetRemainsId> {

    /**
     * Find all remains for a user
     */
    List<PetRemains> findByUserId(String userId);

    /**
     * Find specific remains
     */
    Optional<PetRemains> findByUserIdAndRemainsIndex(String userId, Integer remainsIndex);

    /**
     * Count remains for a user
     */
    long countByUserId(String userId);

    /**
     * Find highest remains index for a user
     */
    @Query("SELECT COALESCE(MAX(r.remainsIndex), 0) FROM PetRemains r WHERE r.userId = :userId")
    Integer findMaxRemainsIndexByUserId(@Param("userId") String userId);

    /**
     * Delete remains
     */
    void deleteByUserIdAndRemainsIndex(String userId, Integer remainsIndex);

    /**
     * Find remains by type
     */
    List<PetRemains> findByUserIdAndRemainsId(String userId, Integer remainsId);
}
