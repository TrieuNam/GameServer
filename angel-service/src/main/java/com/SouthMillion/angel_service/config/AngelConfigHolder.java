package com.SouthMillion.angel_service.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Angel configuration holder
 * In production, this should load from config-service
 */
@Component
@Data
public class AngelConfigHolder {
    
    // Angel unlock costs (angelId -> cost)
    private final Map<Integer, UnlockCost> unlockCosts = new HashMap<>();
    
    // Level up costs per level
    private final Map<Integer, LevelUpCost> levelUpCosts = new HashMap<>();
    
    // Grade up costs per grade
    private final Map<Integer, GradeUpCost> gradeUpCosts = new HashMap<>();
    
    // Star upgrade costs per star level
    private final Map<Integer, StarUpCost> starUpCosts = new HashMap<>();
    
    // Skill upgrade costs per skill level
    private final Map<Integer, SkillUpCost> skillUpCosts = new HashMap<>();
    
    // Evolution costs per stage
    private final Map<Integer, EvolutionCost> evolutionCosts = new HashMap<>();
    
    public AngelConfigHolder() {
        initializeDefaultConfig();
    }
    
    private void initializeDefaultConfig() {
        // Unlock costs for different angels
        unlockCosts.put(1, new UnlockCost(5000L, 2001, 10));    // Basic angel
        unlockCosts.put(2, new UnlockCost(10000L, 2001, 20));   // Advanced angel
        unlockCosts.put(3, new UnlockCost(50000L, 2001, 50));   // Rare angel
        unlockCosts.put(4, new UnlockCost(100000L, 2001, 100)); // Epic angel
        
        // Level up costs
        for (int level = 1; level <= 100; level++) {
            levelUpCosts.put(level, new LevelUpCost(
                level * 150L,           // Gold cost
                2002,                   // Angel exp stone
                level * 3               // Material quantity
            ));
        }
        
        // Grade up costs
        for (int grade = 1; grade <= 10; grade++) {
            gradeUpCosts.put(grade, new GradeUpCost(
                grade * 2000L,          // Gold cost
                2003,                   // Grade stone
                grade * 8               // Material quantity
            ));
        }
        
        // Star upgrade costs
        for (int star = 0; star < 12; star++) {
            starUpCosts.put(star, new StarUpCost(
                (star + 1) * 800L,      // Gold cost
                2004,                   // Star stone
                (star + 1) * 5          // Material quantity
            ));
        }
        
        // Skill upgrade costs
        for (int skillLevel = 0; skillLevel < 10; skillLevel++) {
            skillUpCosts.put(skillLevel, new SkillUpCost(
                (skillLevel + 1) * 500L, // Gold cost
                2005,                    // Skill book
                (skillLevel + 1) * 2     // Material quantity
            ));
        }
        
        // Evolution costs
        for (int stage = 0; stage < 5; stage++) {
            evolutionCosts.put(stage, new EvolutionCost(
                (stage + 1) * 5000L,     // Gold cost
                2006,                    // Evolution stone
                (stage + 1) * 10         // Material quantity
            ));
        }
    }
    
    public UnlockCost getUnlockCost(Integer angelId) {
        return unlockCosts.getOrDefault(angelId, new UnlockCost(5000L, 2001, 10));
    }
    
    public LevelUpCost getLevelUpCost(Integer level) {
        return levelUpCosts.getOrDefault(level, new LevelUpCost(150L, 2002, 3));
    }
    
    public GradeUpCost getGradeUpCost(Integer grade) {
        return gradeUpCosts.getOrDefault(grade, new GradeUpCost(2000L, 2003, 8));
    }
    
    public StarUpCost getStarUpCost(Integer starLevel) {
        return starUpCosts.getOrDefault(starLevel, new StarUpCost(800L, 2004, 5));
    }
    
    public SkillUpCost getSkillUpCost(Integer skillLevel) {
        return skillUpCosts.getOrDefault(skillLevel, new SkillUpCost(500L, 2005, 2));
    }
    
    public EvolutionCost getEvolutionCost(Integer stage) {
        return evolutionCosts.getOrDefault(stage, new EvolutionCost(5000L, 2006, 10));
    }
    
    @Data
    public static class UnlockCost {
        private final long goldCost;
        private final int materialItemId;
        private final int materialQuantity;
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
    public static class SkillUpCost {
        private final long goldCost;
        private final int materialItemId;
        private final int materialQuantity;
    }
    
    @Data
    public static class EvolutionCost {
        private final long goldCost;
        private final int materialItemId;
        private final int materialQuantity;
    }
}
