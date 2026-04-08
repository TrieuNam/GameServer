package com.SouthMillion.rune_service.util;

import com.SouthMillion.rune_service.config.RuneConfigProvider;
import com.SouthMillion.rune_service.model.config.RuneConfig;
import com.SouthMillion.rune_service.model.entity.Rune;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Rune Power Calculator
 * Calculates rune combat power based on config data
 *
 * Formula: (main_attr + sub_attrs) × quality_multiplier × star_multiplier × refinement_bonus
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RunePowerCalculator {

    private final RuneConfigProvider configProvider;

    // Attribute weights (same as mount-service for consistency)
    private static final int HP_WEIGHT = 1;
    private static final int ATK_WEIGHT = 5;
    private static final int DEF_WEIGHT = 3;
    private static final int SPD_WEIGHT = 2;
    private static final int CRIT_RATE_WEIGHT = 10;  // Crit rate is valuable
    private static final int CRIT_DMG_WEIGHT = 8;    // Crit damage is valuable

    /**
     * Calculate total rune power
     *
     * @param rune Rune entity
     * @return Total rune power
     */
    public Long calculateTotalRunePower(Rune rune) {
        if (rune == null) {
            return 0L;
        }

        // Get base attributes from config
        RuneConfig.Attributes baseAttrs = getRuneBaseAttributes(rune.getRuneId());

        // Calculate base power from main attribute and sub attributes
        long basePower = calculateBasePower(baseAttrs, rune);

        // Calculate multipliers
        double qualityMultiplier = calculateQualityMultiplier(rune.getQuality());
        double starMultiplier = calculateStarMultiplier(rune.getStar());
        double refinementBonus = calculateRefinementBonus(rune.getRefinementLevel());

        // Total power = base × quality × star × (1 + refinement)
        long totalPower = Math.round(basePower * qualityMultiplier * starMultiplier * (1.0 + refinementBonus));

        log.debug("Rune power calculated: base={}, quality={}x, star={}x, refinement=+{}%, total={}",
                basePower, qualityMultiplier, starMultiplier, (int)(refinementBonus * 100), totalPower);

        return totalPower;
    }

    /**
     * Calculate base power from attributes
     */
    private long calculateBasePower(RuneConfig.Attributes baseAttrs, Rune rune) {
        int level = rune.getLevel();

        // Get current stats (base + level growth)
        int currentHp = (baseAttrs.getHp() != null ? baseAttrs.getHp() : 50) + (level - 1) * 10;
        int currentAttack = (baseAttrs.getAttack() != null ? baseAttrs.getAttack() : 25) + (level - 1) * 5;
        int currentDefense = (baseAttrs.getDefense() != null ? baseAttrs.getDefense() : 15) + (level - 1) * 3;
        int currentSpeed = (baseAttrs.getSpeed() != null ? baseAttrs.getSpeed() : 10) + (level - 1) * 2;
        int currentCritRate = (baseAttrs.getCritRate() != null ? baseAttrs.getCritRate() : 0);
        int currentCritDmg = (baseAttrs.getCritDamage() != null ? baseAttrs.getCritDamage() : 0);

        // Calculate main attribute power (weighted)
        long mainAttrPower = rune.getMainAttrValue() * getAttributeWeight(rune.getMainAttrType());

        // Calculate sub attributes power
        long subAttrPower = 0L;
        if (rune.getSubAttr1Type() != null && rune.getSubAttr1Value() != null) {
            subAttrPower += rune.getSubAttr1Value() * getAttributeWeight(rune.getSubAttr1Type());
        }
        if (rune.getSubAttr2Type() != null && rune.getSubAttr2Value() != null) {
            subAttrPower += rune.getSubAttr2Value() * getAttributeWeight(rune.getSubAttr2Type());
        }
        if (rune.getSubAttr3Type() != null && rune.getSubAttr3Value() != null) {
            subAttrPower += rune.getSubAttr3Value() * getAttributeWeight(rune.getSubAttr3Type());
        }

        // Base power from config stats
        long configPower = (currentHp * HP_WEIGHT) +
                          (currentAttack * ATK_WEIGHT) +
                          (currentDefense * DEF_WEIGHT) +
                          (currentSpeed * SPD_WEIGHT) +
                          (currentCritRate * CRIT_RATE_WEIGHT) +
                          (currentCritDmg * CRIT_DMG_WEIGHT);

        return configPower + mainAttrPower + subAttrPower;
    }

    /**
     * Get attribute weight based on type
     * Types: 1=HP, 2=ATK, 3=DEF, 4=SPD, 5=CRIT_RATE, 6=CRIT_DMG, etc.
     */
    private int getAttributeWeight(Integer attrType) {
        if (attrType == null) return 1;

        return switch (attrType) {
            case 1 -> HP_WEIGHT;        // HP
            case 2 -> ATK_WEIGHT;       // Attack
            case 3 -> DEF_WEIGHT;       // Defense
            case 4 -> SPD_WEIGHT;       // Speed
            case 5 -> CRIT_RATE_WEIGHT; // Crit Rate
            case 6 -> CRIT_DMG_WEIGHT;  // Crit Damage
            default -> 1;               // Other attributes
        };
    }

    /**
     * Calculate quality multiplier
     * Quality 1-5 (White, Green, Blue, Purple, Orange)
     * Each quality adds 25% multiplier
     */
    private double calculateQualityMultiplier(Integer quality) {
        if (quality == null || quality < 1) {
            return 1.0;
        }
        // Quality 1 = 1.0x, Quality 2 = 1.25x, Quality 3 = 1.5x, Quality 4 = 1.75x, Quality 5 = 2.0x
        return 1.0 + (quality - 1) * 0.25;
    }

    /**
     * Calculate star multiplier
     * Each star adds 15% multiplier (more valuable than mount stars)
     */
    private double calculateStarMultiplier(Integer star) {
        if (star == null || star < 1) {
            return 1.0;
        }
        // Star 1 = 1.0x, Star 2 = 1.15x, Star 3 = 1.3x, etc.
        return 1.0 + (star - 1) * 0.15;
    }

    /**
     * Calculate refinement bonus
     * Each refinement level adds 2% bonus (additive)
     */
    private double calculateRefinementBonus(Integer refinementLevel) {
        if (refinementLevel == null || refinementLevel < 1) {
            return 0.0;
        }
        // Refinement 1 = +2%, Refinement 5 = +10%, Refinement 10 = +20%, etc.
        return refinementLevel * 0.02;
    }

    /**
     * Get rune base attributes from config
     */
    private RuneConfig.Attributes getRuneBaseAttributes(Integer runeId) {
        try {
            RuneConfig config = configProvider.getRuneConfig();
            if (config != null && config.getRuneList() != null) {
                for (RuneConfig.RuneItem item : config.getRuneList()) {
                    if (item.getRuneId().equals(runeId)) {
                        return item.getBaseAttributes();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get rune base attributes from config: {}", e.getMessage());
        }

        // Return default attributes if config not available
        return new RuneConfig.Attributes(50, 25, 15, 10, 0, 0);
    }
}
