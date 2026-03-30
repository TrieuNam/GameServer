package com.SouthMillion.angel_service.repository;

import com.SouthMillion.angel_service.model.entity.Angel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AngelRepository extends JpaRepository<Angel, Long> {
    
    /**
     * Find all angels for user
     */
    List<Angel> findByUserId(Long userId);
    
    /**
     * Find specific angel by user and index
     */
    Optional<Angel> findByUserIdAndAngelIndex(Long userId, Integer angelIndex);
    
    /**
     * Find currently equipped angel
     */
    Optional<Angel> findByUserIdAndIsEquippedTrue(Long userId);
    
    /**
     * Find all active/unlocked angels
     */
    List<Angel> findByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Count active angels
     */
    long countByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Unequip all angels before equipping new one
     */
    @Modifying
    @Query("UPDATE Angel a SET a.isEquipped = false WHERE a.userId = :userId AND a.isEquipped = true")
    void unequipAllAngels(@Param("userId") Long userId);
    
    /**
     * Check if angel exists
     */
    boolean existsByUserIdAndAngelIndex(Long userId, Integer angelIndex);
    
    /**
     * Find by angel ID (config ID)
     */
    Optional<Angel> findByUserIdAndAngelId(Long userId, Integer angelId);
}
