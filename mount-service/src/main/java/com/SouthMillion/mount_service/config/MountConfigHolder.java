package com.SouthMillion.mount_service.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mount configuration holder
 * In production, this should load from config-service
 */
@Component
@Data
public class MountConfigHolder {
    
    // Mount unlock costs (mountId -> gold cost)
    private final Map<Integer, Long> unlockCosts = new HashMap<>();
    
    // Level up costs per level
    private final Map<Integer, LevelUpCost> levelUpCosts = new HashMap<>();
    
    // Grade up costs per grade
    private final Map<Integer, GradeUpCost> gradeUpCosts = new HashMap<>();
    
    // Star upgrade costs per star level
    private final Map<Integer, StarUpCost> starUpCosts = new HashMap<>();
    
    public MountConfigHolder() {
        initializeDefaultConfig();
    }
    
    private void initializeDefaultConfig() {
        // Unlock costs for different mounts
        unlockCosts.put(1, 1000L);   // Basic mount: 1000 gold
        unlockCosts.put(2, 5000L);   // Advanced mount: 5000 gold
        unlockCosts.put(3, 10000L);  // Rare mount: 10000 gold
        unlockCosts.put(4, 50000L);  // Epic mount: 50000 gold
        
        // Level up costs (level -> cost)
        for (int level = 1; level <= 100; level++) {
            levelUpCosts.put(level, new LevelUpCost(
                level * 100L,           // Gold cost
                1001,                   // Material item ID (mount exp stone)
                level * 2               // Material quantity
            ));
        }
        
        // Grade up costs (grade -> cost)
        for (int grade = 1; grade <= 10; grade++) {
            gradeUpCosts.put(grade, new GradeUpCost(
                grade * 1000L,          // Gold cost
                1002,                   // Material item ID (grade stone)
                grade * 5               // Material quantity
            ));
        }
        
        // Star upgrade costs (star -> cost)
        for (int star = 0; star < 10; star++) {
            starUpCosts.put(star, new StarUpCost(
                (star + 1) * 500L,      // Gold cost
                1003,                   // Material item ID (star stone)
                (star + 1) * 3          // Material quantity
            ));
        }
    }
    
    public long getUnlockCost(Integer mountId) {
        return unlockCosts.getOrDefault(mountId, 1000L);
    }
    
    public LevelUpCost getLevelUpCost(Integer level) {
        return levelUpCosts.getOrDefault(level, new LevelUpCost(100L, 1001, 2));
    }
    
    public GradeUpCost getGradeUpCost(Integer grade) {
        return gradeUpCosts.getOrDefault(grade, new GradeUpCost(1000L, 1002, 5));
    }
    
    public StarUpCost getStarUpCost(Integer starLevel) {
        return starUpCosts.getOrDefault(starLevel, new StarUpCost(500L, 1003, 3));
    }
    
    @Data
    public static class LevelUpCost {
        private final long goldCost;
        private final int materialItemId;
        private final int materialQuantity;
    }
    
    @Data
    public static class GradeUpCost {
        private final long goldCost;
        private final int materialItemId;
        private final int materialQuantity;
    }
    
    @Data
    public static class StarUpCost {
        private final long goldCost;
        private final int materialItemId;
        private final int materialQuantity;
    }
    
    @Data
    public static class SkinUpgradeCost {
        private final long diamondCost;
        private final int materialItemId;
        private final int materialQuantity;
    }
    
    @Data
    public static class HarnessBuyCost {
        private final long goldCost;
        private final long diamondCost;
        private final int materialItemId;
        private final int materialQuantity;
    }
    
    public HarnessBuyCost getHarnessBuyCost(Integer buyType) {
        if (buyType == 1) {
            // Gold buy
            return new HarnessBuyCost(1000L, 0L, 0, 0);
        } else {
            // Diamond buy
            return new HarnessBuyCost(0L, 500L, 0, 0);
        }
    }
}
