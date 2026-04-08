package com.SouthMillion.mount_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mount Skill/Buff model
 * Represents passive bonuses that mounts provide to the player and party
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MountSkill {

    /**
     * Skill ID from config
     */
    private Integer skillId;

    /**
     * Skill name
     */
    private String skillName;

    /**
     * Skill type: PERSONAL, PARTY, GUILD
     */
    private SkillType skillType;

    /**
     * Buff attribute type: HP, ATK, DEF, SPD, CRIT_RATE, CRIT_DMG, etc.
     */
    private BuffAttribute buffAttribute;

    /**
     * Buff value (fixed amount or percentage)
     */
    private Long buffValue;

    /**
     * Whether the buff is percentage-based
     */
    private Boolean isPercentage;

    /**
     * Mount grade requirement to unlock this skill
     */
    private Integer requiredGrade;

    /**
     * Mount star level requirement to unlock this skill
     */
    private Integer requiredStarLevel;

    public enum SkillType {
        PERSONAL,   // Affects only the mount owner
        PARTY,      // Affects all party members
        GUILD       // Affects all guild members
    }

    public enum BuffAttribute {
        HP,
        ATK,
        DEF,
        SPD,
        CRIT_RATE,
        CRIT_DMG,
        DODGE,
        HIT_RATE,
        EXP_BONUS,
        GOLD_BONUS,
        DROP_RATE
    }

    /**
     * Check if this skill is unlocked for the given mount
     */
    public boolean isUnlocked(Integer mountGrade, Integer mountStarLevel) {
        if (requiredGrade != null && mountGrade < requiredGrade) {
            return false;
        }
        if (requiredStarLevel != null && mountStarLevel < requiredStarLevel) {
            return false;
        }
        return true;
    }

    /**
     * Get display value for UI
     */
    public String getDisplayValue() {
        if (isPercentage) {
            return buffValue + "%";
        } else {
            return "+" + buffValue;
        }
    }
}
