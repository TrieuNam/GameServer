package com.SouthMillion.starmap_service.util;

import com.SouthMillion.starmap_service.model.config.StarmapConfig;
import com.SouthMillion.starmap_service.model.entity.Constellation;
import com.SouthMillion.starmap_service.model.entity.Star;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility for calculating starmap power (celestial power)
 *
 * Power Formula:
 * - Star Power = (base_attributes × level_multiplier) + blessing
 * - Constellation Power = (bonus_attributes × level_multiplier) + (completed_bonus × completion_rate)
 * - Total Power = sum(all active stars) + sum(all unlocked constellations)
 *
 * Level Multiplier:
 * - Stars: 1.0 + (level - 1) × 0.05 (5% per level)
 * - Constellations: 1.0 + (level - 1) × 0.10 (10% per level)
 *
 * Attribute Weights:
 * - HP: 1x
 * - Attack: 5x
 * - Defense: 3x
 * - Speed: 2x
 * - Crit Rate: 10x
 * - Crit Damage: 8x
 * - Blessing: 15x (special celestial power)
 */
@Component
@Slf4j
public class StarmapPowerCalculator {

    // Attribute weights for power calculation
    private static final int HP_WEIGHT = 1;
    private static final int ATTACK_WEIGHT = 5;
    private static final int DEFENSE_WEIGHT = 3;
    private static final int SPEED_WEIGHT = 2;
    private static final int CRIT_RATE_WEIGHT = 10;
    private static final int CRIT_DAMAGE_WEIGHT = 8;
    private static final int BLESSING_WEIGHT = 15; // Special celestial power

    // Level multipliers
    private static final double STAR_LEVEL_MULTIPLIER = 0.05; // 5% per level
    private static final double CONSTELLATION_LEVEL_MULTIPLIER = 0.10; // 10% per level

    /**
     * Calculate total power for a single star
     *
     * @param star Star entity
     * @param starConfig Star configuration (optional, for base attributes)
     * @return Total power value
     */
    public Long calculateStarPower(Star star, StarmapConfig.StarItem starConfig) {
        if (star == null || !star.getIsActive()) {
            return 0L;
        }

        // If config not available, return simple calculation based on level
        if (starConfig == null || starConfig.getBaseAttributes() == null) {
            log.warn("No config for star {}, using fallback calculation", star.getStarId());
            return calculateFallbackStarPower(star.getLevel());
        }

        StarmapConfig.Attributes attrs = starConfig.getBaseAttributes();

        // Calculate base power from attributes
        long basePower = calculateAttributePower(attrs);

        // Apply level multiplier
        double levelMultiplier = calculateStarLevelMultiplier(star.getLevel());

        return Math.round(basePower * levelMultiplier);
    }

    /**
     * Calculate total power for a constellation
     *
     * @param constellation Constellation entity
     * @param constellationConfig Constellation configuration (optional)
     * @return Total power value
     */
    public Long calculateConstellationPower(Constellation constellation,
                                           StarmapConfig.ConstellationItem constellationConfig) {
        if (constellation == null || !constellation.getIsUnlocked()) {
            return 0L;
        }

        // If config not available, use fallback
        if (constellationConfig == null || constellationConfig.getBonusAttributes() == null) {
            log.warn("No config for constellation {}, using fallback", constellation.getConstellationId());
            return calculateFallbackConstellationPower(constellation.getLevel(), constellation.getIsCompleted());
        }

        StarmapConfig.Attributes attrs = constellationConfig.getBonusAttributes();

        // Calculate base power from bonus attributes
        long basePower = calculateAttributePower(attrs);

        // Apply level multiplier
        double levelMultiplier = calculateConstellationLevelMultiplier(constellation.getLevel());

        long power = Math.round(basePower * levelMultiplier);

        // Completion bonus: 50% extra power when all stars are activated
        if (constellation.getIsCompleted()) {
            power = Math.round(power * 1.5);
        }

        return power;
    }

    /**
     * Calculate total starmap power for a user
     *
     * @param stars List of user's stars
     * @param constellations List of user's constellations
     * @param starmapConfig Starmap configuration
     * @return Total celestial power
     */
    public Long calculateTotalStarmapPower(List<Star> stars,
                                           List<Constellation> constellations,
                                           StarmapConfig starmapConfig) {
        long totalPower = 0L;

        // Add power from all active stars
        if (stars != null) {
            for (Star star : stars) {
                if (!star.getIsActive()) {
                    continue;
                }

                StarmapConfig.StarItem starConfig = findStarConfig(star.getStarId(), starmapConfig);
                long starPower = calculateStarPower(star, starConfig);
                totalPower += starPower;
            }
        }

        // Add power from all unlocked constellations
        if (constellations != null) {
            for (Constellation constellation : constellations) {
                if (!constellation.getIsUnlocked()) {
                    continue;
                }

                StarmapConfig.ConstellationItem constellationConfig =
                    findConstellationConfig(constellation.getConstellationId(), starmapConfig);
                long constellationPower = calculateConstellationPower(constellation, constellationConfig);
                totalPower += constellationPower;
            }
        }

        return totalPower;
    }

    /**
     * Calculate power from attributes with weights
     */
    private long calculateAttributePower(StarmapConfig.Attributes attrs) {
        long power = 0L;

        if (attrs.getHp() != null) {
            power += attrs.getHp() * HP_WEIGHT;
        }
        if (attrs.getAttack() != null) {
            power += attrs.getAttack() * ATTACK_WEIGHT;
        }
        if (attrs.getDefense() != null) {
            power += attrs.getDefense() * DEFENSE_WEIGHT;
        }
        if (attrs.getSpeed() != null) {
            power += attrs.getSpeed() * SPEED_WEIGHT;
        }
        if (attrs.getCritRate() != null) {
            power += attrs.getCritRate() * CRIT_RATE_WEIGHT;
        }
        if (attrs.getCritDamage() != null) {
            power += attrs.getCritDamage() * CRIT_DAMAGE_WEIGHT;
        }
        if (attrs.getBlessing() != null) {
            power += attrs.getBlessing() * BLESSING_WEIGHT;
        }

        return power;
    }

    /**
     * Calculate star level multiplier
     * Formula: 1.0 + (level - 1) × 0.05
     */
    private double calculateStarLevelMultiplier(Integer level) {
        if (level == null || level <= 0) {
            return 1.0;
        }
        return 1.0 + (level - 1) * STAR_LEVEL_MULTIPLIER;
    }

    /**
     * Calculate constellation level multiplier
     * Formula: 1.0 + (level - 1) × 0.10
     */
    private double calculateConstellationLevelMultiplier(Integer level) {
        if (level == null || level <= 0) {
            return 1.0;
        }
        return 1.0 + (level - 1) * CONSTELLATION_LEVEL_MULTIPLIER;
    }

    /**
     * Fallback star power calculation when config unavailable
     */
    private Long calculateFallbackStarPower(Integer level) {
        if (level == null || level <= 0) {
            return 0L;
        }
        // Simple formula: 100 base × level × 1.2^level
        return Math.round(100 * level * Math.pow(1.2, level));
    }

    /**
     * Fallback constellation power calculation when config unavailable
     */
    private Long calculateFallbackConstellationPower(Integer level, Boolean isCompleted) {
        if (level == null || level <= 0) {
            return 0L;
        }
        long power = Math.round(500 * level * Math.pow(1.3, level));
        return isCompleted ? Math.round(power * 1.5) : power;
    }

    /**
     * Find star config by starId
     */
    private StarmapConfig.StarItem findStarConfig(Integer starId, StarmapConfig config) {
        if (config == null || config.getStars() == null) {
            return null;
        }

        return config.getStars().stream()
            .filter(s -> s.getStarId().equals(starId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find constellation config by constellationId
     */
    private StarmapConfig.ConstellationItem findConstellationConfig(Integer constellationId,
                                                                    StarmapConfig config) {
        if (config == null || config.getConstellations() == null) {
            return null;
        }

        return config.getConstellations().stream()
            .filter(c -> c.getConstellationId().equals(constellationId))
            .findFirst()
            .orElse(null);
    }
}
