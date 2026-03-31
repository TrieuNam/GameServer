package com.SouthMillion.mount_service.repository;

import com.SouthMillion.mount_service.model.entity.Mount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MountRepository extends JpaRepository<Mount, Long> {
    
    /**
     * Find all mounts for a user
     */
    List<Mount> findByUserId(Long userId);
    
    /**
     * Find specific mount by user and index
     */
    Optional<Mount> findByUserIdAndMountIndex(Long userId, Integer mountIndex);
    
    /**
     * Find currently equipped mount
     */
    Optional<Mount> findByUserIdAndIsEquippedTrue(Long userId);
    
    /**
     * Find all active/unlocked mounts
     */
    List<Mount> findByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Count active mounts for user
     */
    long countByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Unequip all mounts for user (before equipping new one)
     */
    @Modifying
    @Query("UPDATE Mount m SET m.isEquipped = false WHERE m.userId = :userId AND m.isEquipped = true")
    void unequipAllMounts(@Param("userId") Long userId);
    
    /**
     * Update harness slot
     */
    @Modifying
    @Query("UPDATE Mount m SET m.harnessSlot1 = CASE WHEN :slotIndex = 1 THEN :itemId ELSE m.harnessSlot1 END, " +
           "m.harnessSlot2 = CASE WHEN :slotIndex = 2 THEN :itemId ELSE m.harnessSlot2 END, " +
           "m.harnessSlot3 = CASE WHEN :slotIndex = 3 THEN :itemId ELSE m.harnessSlot3 END, " +
           "m.harnessSlot4 = CASE WHEN :slotIndex = 4 THEN :itemId ELSE m.harnessSlot4 END " +
           "WHERE m.userId = :userId AND m.mountIndex = :mountIndex")
    void updateHarnessSlot(@Param("userId") Long userId, 
                          @Param("mountIndex") Integer mountIndex,
                          @Param("slotIndex") Integer slotIndex,
                          @Param("itemId") Integer itemId);
    
    /**
     * Check if mount exists
     */
    boolean existsByUserIdAndMountIndex(Long userId, Integer mountIndex);
}
