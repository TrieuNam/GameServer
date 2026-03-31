package com.SouthMillion.rune_service.repository;

import com.SouthMillion.rune_service.model.entity.Rune;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuneRepository extends JpaRepository<Rune, Long> {
    
    List<Rune> findByUserId(Long userId);
    
    Optional<Rune> findByUserIdAndRuneIndex(Long userId, Integer runeIndex);
    
    List<Rune> findByUserIdAndIsEquippedTrue(Long userId);
    
    Optional<Rune> findByUserIdAndEquipSlot(Long userId, Integer equipSlot);
    
    long countByUserId(Long userId);
    
    boolean existsByUserIdAndRuneIndex(Long userId, Integer runeIndex);
    
    @Query("SELECT COALESCE(MAX(r.runeIndex), 0) FROM Rune r WHERE r.userId = :userId")
    Integer findMaxRuneIndexByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Rune r SET r.isEquipped = false, r.equipSlot = null WHERE r.userId = :userId AND r.equipSlot = :equipSlot")
    void unequipRuneFromSlot(@Param("userId") Long userId, @Param("equipSlot") Integer equipSlot);
}
