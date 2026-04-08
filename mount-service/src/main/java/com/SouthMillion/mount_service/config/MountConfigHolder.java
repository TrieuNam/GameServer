package com.SouthMillion.mount_service.config;

import com.SouthMillion.mount_service.model.config.HarnessConfig;
import com.SouthMillion.mount_service.model.config.MountConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mount configuration holder
 * Now integrates with MountConfigProvider to load from config-service
 * Falls back to hardcoded defaults if config is not available
 */
@Component
@Data
@RequiredArgsConstructor
@Slf4j
public class MountConfigHolder {

    private final MountConfigProvider configProvider;

    // Fallback default costs (used when config is not loaded)
    private final Map<Integer, Long> defaultUnlockCosts = new HashMap<>();
    private final Map<Integer, LevelUpCost> defaultLevelUpCosts = new HashMap<>();
    private final Map<Integer, GradeUpCost> defaultGradeUpCosts = new HashMap<>();
    private final Map<Integer, StarUpCost> defaultStarUpCosts = new HashMap<>();

    public MountConfigHolder(MountConfigProvider configProvider) {
        this.configProvider = configProvider;
        initializeDefaultConfig();
    }

    private void initializeDefaultConfig() {
        // Unlock costs for different mounts
        defaultUnlockCosts.put(1, 1000L);   // Basic mount: 1000 gold
        defaultUnlockCosts.put(2, 5000L);   // Advanced mount: 5000 gold
        defaultUnlockCosts.put(3, 10000L);  // Rare mount: 10000 gold
        defaultUnlockCosts.put(4, 50000L);  // Epic mount: 50000 gold

        // Level up costs (level -> cost)
        for (int level = 1; level <= 100; level++) {
            defaultLevelUpCosts.put(level, new LevelUpCost(
                level * 100L,           // Gold cost
                1001,                   // Material item ID (mount exp stone)
                level * 2               // Material quantity
            ));
        }

        // Grade up costs (grade -> cost)
        for (int grade = 1; grade <= 10; grade++) {
            defaultGradeUpCosts.put(grade, new GradeUpCost(
                grade * 1000L,          // Gold cost
                1002,                   // Material item ID (grade stone)
                grade * 5               // Material quantity
            ));
        }

        // Star upgrade costs (star -> cost)
        for (int star = 0; star < 10; star++) {
            defaultStarUpCosts.put(star, new StarUpCost(
                (star + 1) * 500L,      // Gold cost
                1003,                   // Material item ID (star stone)
                (star + 1) * 3          // Material quantity
            ));
        }
    }

    /**
     * Get unlock cost for a mount
     * Tries config first, falls back to defaults
     */
    public long getUnlockCost(Integer mountId) {
        try {
            MountConfig mountConfig = configProvider.getMountConfig();
            if (mountConfig != null && mountConfig.getMountList() != null) {
                for (MountConfig.MountItem item : mountConfig.getMountList()) {
                    if (item.getMountId().equals(mountId)) {
                        return item.getUnlockCost() != null ? item.getUnlockCost() : 1000L;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get unlock cost from config, using default: {}", e.getMessage());
        }

        return defaultUnlockCosts.getOrDefault(mountId, 1000L);
    }

    /**
     * Get level up cost
     * Tries config first, falls back to defaults
     */
    public LevelUpCost getLevelUpCost(Integer level) {
        try {
            MountConfig mountConfig = configProvider.getMountConfig();
            if (mountConfig != null && mountConfig.getLevelCosts() != null) {
                for (MountConfig.LevelCost cost : mountConfig.getLevelCosts()) {
                    if (cost.getLevel().equals(level)) {
                        return new LevelUpCost(
                            cost.getGold() != null ? cost.getGold() : 100L,
                            cost.getMaterialId() != null ? cost.getMaterialId() : 1001,
                            cost.getMaterialCount() != null ? cost.getMaterialCount() : 2
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get level up cost from config, using default: {}", e.getMessage());
        }

        return defaultLevelUpCosts.getOrDefault(level, new LevelUpCost(100L, 1001, 2));
    }

    /**
     * Get grade up cost
     * Tries config first, falls back to defaults
     */
    public GradeUpCost getGradeUpCost(Integer grade) {
        try {
            MountConfig mountConfig = configProvider.getMountConfig();
            if (mountConfig != null && mountConfig.getGradeCosts() != null) {
                for (MountConfig.GradeCost cost : mountConfig.getGradeCosts()) {
                    if (cost.getGrade().equals(grade)) {
                        return new GradeUpCost(
                            cost.getGold() != null ? cost.getGold() : 1000L,
                            cost.getMaterialId() != null ? cost.getMaterialId() : 1002,
                            cost.getMaterialCount() != null ? cost.getMaterialCount() : 5
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get grade up cost from config, using default: {}", e.getMessage());
        }

        return defaultGradeUpCosts.getOrDefault(grade, new GradeUpCost(1000L, 1002, 5));
    }

    /**
     * Get star upgrade cost
     * Tries config first, falls back to defaults
     */
    public StarUpCost getStarUpCost(Integer starLevel) {
        try {
            MountConfig mountConfig = configProvider.getMountConfig();
            if (mountConfig != null && mountConfig.getStarCosts() != null) {
                for (MountConfig.StarCost cost : mountConfig.getStarCosts()) {
                    if (cost.getStarLevel().equals(starLevel)) {
                        return new StarUpCost(
                            cost.getGold() != null ? cost.getGold() : 500L,
                            cost.getMaterialId() != null ? cost.getMaterialId() : 1003,
                            cost.getMaterialCount() != null ? cost.getMaterialCount() : 3
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get star up cost from config, using default: {}", e.getMessage());
        }

        return defaultStarUpCosts.getOrDefault(starLevel, new StarUpCost(500L, 1003, 3));
    }

    /**
     * Get harness buy cost
     * Tries config first, falls back to defaults
     */
    public HarnessBuyCost getHarnessBuyCost(Integer buyType) {
        try {
            HarnessConfig harnessConfig = configProvider.getHarnessConfig();
            if (harnessConfig != null && harnessConfig.getHarnessList() != null) {
                // Could implement specific harness buy cost logic here if needed
                // For now, use simple logic based on buyType
            }
        } catch (Exception e) {
            log.warn("Failed to get harness buy cost from config, using default: {}", e.getMessage());
        }

        // Default logic
        if (buyType == 1) {
            // Gold buy
            return new HarnessBuyCost(1000L, 0L, 0, 0);
        } else {
            // Diamond buy
            return new HarnessBuyCost(0L, 500L, 0, 0);
        }
    }

    /**
     * Get mount base attributes from config
     */
    public MountConfig.Attributes getMountBaseAttributes(Integer mountId) {
        try {
            MountConfig mountConfig = configProvider.getMountConfig();
            if (mountConfig != null && mountConfig.getMountList() != null) {
                for (MountConfig.MountItem item : mountConfig.getMountList()) {
                    if (item.getMountId().equals(mountId)) {
                        return item.getBaseAttributes();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get mount base attributes from config: {}", e.getMessage());
        }

        // Return default attributes
        return new MountConfig.Attributes(100, 50, 30, 20);
    }

    /**
     * Get mount growth rate from config
     */
    public MountConfig.GrowthRate getMountGrowthRate(Integer mountId) {
        try {
            MountConfig mountConfig = configProvider.getMountConfig();
            if (mountConfig != null && mountConfig.getMountList() != null) {
                for (MountConfig.MountItem item : mountConfig.getMountList()) {
                    if (item.getMountId().equals(mountId)) {
                        return item.getGrowthRate();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get mount growth rate from config: {}", e.getMessage());
        }

        // Return default growth rate
        return new MountConfig.GrowthRate(10, 5, 3, 2);
    }

    /**
     * Get harness attributes from config
     */
    public HarnessConfig.Attributes getHarnessAttributes(Integer harnessId) {
        try {
            HarnessConfig harnessConfig = configProvider.getHarnessConfig();
            if (harnessConfig != null && harnessConfig.getHarnessList() != null) {
                for (HarnessConfig.HarnessItem item : harnessConfig.getHarnessList()) {
                    if (item.getHarnessId().equals(harnessId)) {
                        return item.getBaseAttributes();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get harness attributes from config: {}", e.getMessage());
        }

        // Return default attributes
        return new HarnessConfig.Attributes(50, 25, 15, 10);
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
}
