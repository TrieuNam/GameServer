package com.SouthMillion.mount_service.repository;

import com.SouthMillion.mount_service.model.entity.MountHarness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MountHarnessRepository extends JpaRepository<MountHarness, Long> {
    
    /**
     * Find all harness items for user
     */
    List<MountHarness> findByUserId(Long userId);
    
    /**
     * Find specific harness by user and index
     */
    Optional<MountHarness> findByUserIdAndHarnessIndex(Long userId, Integer harnessIndex);
    
    /**
     * Count harness items in bag
     */
    long countByUserId(Long userId);
    
    /**
     * Find next available index for new harness
     */
    @Query("SELECT COALESCE(MAX(h.harnessIndex), -1) + 1 FROM MountHarness h WHERE h.userId = :userId")
    Integer findNextAvailableIndex(@Param("userId") Long userId);
    
    /**
     * Delete harness by user and index
     */
    void deleteByUserIdAndHarnessIndex(Long userId, Integer harnessIndex);
    
    /**
     * Find harness by grade (for decomposition/upgrade materials)
     */
    List<MountHarness> findByUserIdAndGrade(Long userId, Integer grade);

    /**
     * Find equipped harness items by item IDs
     * Used to fetch harness details for mounted equipment slots
     */
    @Query("SELECT h FROM MountHarness h WHERE h.userId = :userId AND h.itemId IN :itemIds")
    List<MountHarness> findByUserIdAndItemIdIn(@Param("userId") Long userId, @Param("itemIds") List<Integer> itemIds);
}
