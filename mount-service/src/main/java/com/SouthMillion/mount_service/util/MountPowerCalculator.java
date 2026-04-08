package com.SouthMillion.mount_service.util;

import com.SouthMillion.mount_service.config.MountConfigHolder;
import com.SouthMillion.mount_service.model.config.HarnessConfig;
import com.SouthMillion.mount_service.model.config.MountConfig;
import com.SouthMillion.mount_service.model.entity.Mount;
import com.SouthMillion.mount_service.model.entity.MountHarness;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mount power calculator utility
 * Calculates mount combat power based on config data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MountPowerCalculator {

    private final MountConfigHolder configHolder;

    /**
     * Calculate total mount power including base stats, level, grade, star, and harness bonuses
     *
     * @param mount Mount entity
     * @param harnessEquipment List of equipped harness
     * @return Total mount power
     */
    public Long calculateTotalMountPower(Mount mount, List<MountHarness> harnessEquipment) {
        if (mount == null || !mount.getIsActive()) {
            return 0L;
        }

        // Get base attributes from config
        MountConfig.Attributes baseAttrs = configHolder.getMountBaseAttributes(mount.getMountId());
        MountConfig.GrowthRate growthRate = configHolder.getMountGrowthRate(mount.getMountId());

        // Calculate base power from mount stats
        long basePower = calculateBasePower(baseAttrs, growthRate, mount);

        // Calculate harness bonus power
        long harnessPower = calculateHarnessPower(harnessEquipment);

        // Calculate multipliers from grade and star
        double gradeMultiplier = calculateGradeMultiplier(mount.getGrade());
        double starMultiplier = calculateStarMultiplier(mount.getStarLevel());

        // Total power = (base + harness) * grade multiplier * star multiplier
        long totalPower = Math.round((basePower + harnessPower) * gradeMultiplier * starMultiplier);

        log.debug("Mount power calculated: base={}, harness={}, grade={}x, star={}x, total={}",
                basePower, harnessPower, gradeMultiplier, starMultiplier, totalPower);

        return totalPower;
    }

    /**
     * Calculate base mount power from attributes and level
     */
    private long calculateBasePower(MountConfig.Attributes baseAttrs,
                                     MountConfig.GrowthRate growthRate,
                                     Mount mount) {
        int level = mount.getLevel();

        // Calculate current stats based on level growth
        int currentHp = (baseAttrs.getHp() != null ? baseAttrs.getHp() : 100)
                + (growthRate.getHpPerLevel() != null ? growthRate.getHpPerLevel() : 10) * (level - 1);

        int currentAttack = (baseAttrs.getAttack() != null ? baseAttrs.getAttack() : 50)
                + (growthRate.getAttackPerLevel() != null ? growthRate.getAttackPerLevel() : 5) * (level - 1);

        int currentDefense = (baseAttrs.getDefense() != null ? baseAttrs.getDefense() : 30)
                + (growthRate.getDefensePerLevel() != null ? growthRate.getDefensePerLevel() : 3) * (level - 1);

        int currentSpeed = (baseAttrs.getSpeed() != null ? baseAttrs.getSpeed() : 20)
                + (growthRate.getSpeedPerLevel() != null ? growthRate.getSpeedPerLevel() : 2) * (level - 1);

        // Power formula: HP * 1 + ATK * 5 + DEF * 3 + SPD * 2
        // This weights attack more heavily than other stats
        long power = currentHp + (currentAttack * 5L) + (currentDefense * 3L) + (currentSpeed * 2L);

        // Add skin level bonus (50 power per skin level)
        power += mount.getSkinLevel() * 50L;

        return power;
    }

    /**
     * Calculate power bonus from equipped harness
     */
    private long calculateHarnessPower(List<MountHarness> harnessEquipment) {
        if (harnessEquipment == null || harnessEquipment.isEmpty()) {
            return 0L;
        }

        long totalHarnessPower = 0L;

        for (MountHarness harness : harnessEquipment) {
            if (harness != null && harness.getHarnessId() != null) {
                HarnessConfig.Attributes attrs = configHolder.getHarnessAttributes(harness.getHarnessId());

                if (attrs != null) {
                    // Same power formula as mount stats
                    long harnessPower = (attrs.getHp() != null ? attrs.getHp() : 0)
                            + (attrs.getAttack() != null ? attrs.getAttack() : 0) * 5L
                            + (attrs.getDefense() != null ? attrs.getDefense() : 0) * 3L
                            + (attrs.getSpeed() != null ? attrs.getSpeed() : 0) * 2L;

                    // Apply upgrade level multiplier (each upgrade level adds 10%)
                    if (harness.getUpgradeLevel() != null && harness.getUpgradeLevel() > 0) {
                        harnessPower = Math.round(harnessPower * (1.0 + harness.getUpgradeLevel() * 0.1));
                    }

                    totalHarnessPower += harnessPower;
                }
            }
        }

        return totalHarnessPower;
    }

    /**
     * Calculate grade multiplier
     * Each grade adds 20% bonus
     */
    private double calculateGradeMultiplier(Integer grade) {
        if (grade == null || grade <= 1) {
            return 1.0;
        }
        // Grade 1: 1.0x, Grade 2: 1.2x, Grade 3: 1.4x, etc.
        return 1.0 + (grade - 1) * 0.2;
    }

    /**
     * Calculate star multiplier
     * Each star adds 10% bonus
     */
    private double calculateStarMultiplier(Integer starLevel) {
        if (starLevel == null || starLevel <= 0) {
            return 1.0;
        }
        // Star 0: 1.0x, Star 1: 1.1x, Star 2: 1.2x, etc.
        return 1.0 + starLevel * 0.1;
    }

    /**
     * Calculate mount attributes for display
     */
    public MountAttributes calculateMountAttributes(Mount mount, List<MountHarness> harnessEquipment) {
        if (mount == null || !mount.getIsActive()) {
            return new MountAttributes(0, 0, 0, 0);
        }

        // Get base attributes from config
        MountConfig.Attributes baseAttrs = configHolder.getMountBaseAttributes(mount.getMountId());
        MountConfig.GrowthRate growthRate = configHolder.getMountGrowthRate(mount.getMountId());

        int level = mount.getLevel();

        // Calculate current stats
        int hp = (baseAttrs.getHp() != null ? baseAttrs.getHp() : 100)
                + (growthRate.getHpPerLevel() != null ? growthRate.getHpPerLevel() : 10) * (level - 1);

        int attack = (baseAttrs.getAttack() != null ? baseAttrs.getAttack() : 50)
                + (growthRate.getAttackPerLevel() != null ? growthRate.getAttackPerLevel() : 5) * (level - 1);

        int defense = (baseAttrs.getDefense() != null ? baseAttrs.getDefense() : 30)
                + (growthRate.getDefensePerLevel() != null ? growthRate.getDefensePerLevel() : 3) * (level - 1);

        int speed = (baseAttrs.getSpeed() != null ? baseAttrs.getSpeed() : 20)
                + (growthRate.getSpeedPerLevel() != null ? growthRate.getSpeedPerLevel() : 2) * (level - 1);

        // Add harness bonuses
        if (harnessEquipment != null) {
            for (MountHarness harness : harnessEquipment) {
                if (harness != null && harness.getHarnessId() != null) {
                    HarnessConfig.Attributes attrs = configHolder.getHarnessAttributes(harness.getHarnessId());
                    if (attrs != null) {
                        hp += attrs.getHp() != null ? attrs.getHp() : 0;
                        attack += attrs.getAttack() != null ? attrs.getAttack() : 0;
                        defense += attrs.getDefense() != null ? attrs.getDefense() : 0;
                        speed += attrs.getSpeed() != null ? attrs.getSpeed() : 0;
                    }
                }
            }
        }

        // Apply grade and star multipliers
        double gradeMultiplier = calculateGradeMultiplier(mount.getGrade());
        double starMultiplier = calculateStarMultiplier(mount.getStarLevel());
        double totalMultiplier = gradeMultiplier * starMultiplier;

        hp = (int) Math.round(hp * totalMultiplier);
        attack = (int) Math.round(attack * totalMultiplier);
        defense = (int) Math.round(defense * totalMultiplier);
        speed = (int) Math.round(speed * totalMultiplier);

        return new MountAttributes(hp, attack, defense, speed);
    }

    /**
     * Record class for mount attributes
     */
    public record MountAttributes(int hp, int attack, int defense, int speed) {
    }
}
