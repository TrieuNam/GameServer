package com.SouthMillion.mount_service.util;

import com.SouthMillion.mount_service.model.MountSkill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mount Skill Provider
 * Provides default mount skill configurations
 * In Phase 3, this would load from config-service
 */
@Component
@Slf4j
public class MountSkillProvider {

    /**
     * Get all available skills for a mount ID
     * These would normally come from config files
     */
    public List<MountSkill> getMountSkills(Integer mountId) {
        List<MountSkill> skills = new ArrayList<>();

        // Default skills that all mounts get
        skills.add(MountSkill.builder()
                .skillId(1001)
                .skillName("Mount Speed Boost")
                .skillType(MountSkill.SkillType.PERSONAL)
                .buffAttribute(MountSkill.BuffAttribute.SPD)
                .buffValue(10L)
                .isPercentage(true)
                .requiredGrade(1)
                .requiredStarLevel(0)
                .build());

        // Grade-locked skills
        skills.add(MountSkill.builder()
                .skillId(1002)
                .skillName("Mount Vigor")
                .skillType(MountSkill.SkillType.PERSONAL)
                .buffAttribute(MountSkill.BuffAttribute.HP)
                .buffValue(500L)
                .isPercentage(false)
                .requiredGrade(3)
                .requiredStarLevel(0)
                .build());

        skills.add(MountSkill.builder()
                .skillId(1003)
                .skillName("Cavalry Charge")
                .skillType(MountSkill.SkillType.PERSONAL)
                .buffAttribute(MountSkill.BuffAttribute.ATK)
                .buffValue(5L)
                .isPercentage(true)
                .requiredGrade(5)
                .requiredStarLevel(0)
                .build());

        // Star-locked skills
        skills.add(MountSkill.builder()
                .skillId(1004)
                .skillName("Battle Aura")
                .skillType(MountSkill.SkillType.PARTY)
                .buffAttribute(MountSkill.BuffAttribute.ATK)
                .buffValue(3L)
                .isPercentage(true)
                .requiredGrade(5)
                .requiredStarLevel(3)
                .build());

        skills.add(MountSkill.builder()
                .skillId(1005)
                .skillName("Fortune's Blessing")
                .skillType(MountSkill.SkillType.PERSONAL)
                .buffAttribute(MountSkill.BuffAttribute.EXP_BONUS)
                .buffValue(10L)
                .isPercentage(true)
                .requiredGrade(7)
                .requiredStarLevel(5)
                .build());

        skills.add(MountSkill.builder()
                .skillId(1006)
                .skillName("Guild Banner")
                .skillType(MountSkill.SkillType.GUILD)
                .buffAttribute(MountSkill.BuffAttribute.DEF)
                .buffValue(5L)
                .isPercentage(true)
                .requiredGrade(10)
                .requiredStarLevel(8)
                .build());

        // High-end mounts get additional special skills
        if (mountId >= 3) { // Rare mounts and above
            skills.add(MountSkill.builder()
                    .skillId(1007)
                    .skillName("Treasure Hunter")
                    .skillType(MountSkill.SkillType.PERSONAL)
                    .buffAttribute(MountSkill.BuffAttribute.DROP_RATE)
                    .buffValue(15L)
                    .isPercentage(true)
                    .requiredGrade(8)
                    .requiredStarLevel(6)
                    .build());
        }

        if (mountId >= 4) { // Epic mounts
            skills.add(MountSkill.builder()
                    .skillId(1008)
                    .skillName("Critical Strike Mastery")
                    .skillType(MountSkill.SkillType.PERSONAL)
                    .buffAttribute(MountSkill.BuffAttribute.CRIT_RATE)
                    .buffValue(10L)
                    .isPercentage(true)
                    .requiredGrade(9)
                    .requiredStarLevel(7)
                    .build());

            skills.add(MountSkill.builder()
                    .skillId(1009)
                    .skillName("Deadly Precision")
                    .skillType(MountSkill.SkillType.PERSONAL)
                    .buffAttribute(MountSkill.BuffAttribute.CRIT_DMG)
                    .buffValue(20L)
                    .isPercentage(true)
                    .requiredGrade(10)
                    .requiredStarLevel(9)
                    .build());
        }

        return skills;
    }

    /**
     * Get skills that are currently active/unlocked for a mount
     */
    public List<MountSkill> getActiveSkills(Integer mountId, Integer mountGrade, Integer mountStarLevel) {
        List<MountSkill> allSkills = getMountSkills(mountId);
        List<MountSkill> activeSkills = new ArrayList<>();

        for (MountSkill skill : allSkills) {
            if (skill.isUnlocked(mountGrade, mountStarLevel)) {
                activeSkills.add(skill);
            }
        }

        log.debug("Mount {} (grade {}, star {}) has {} active skills",
                mountId, mountGrade, mountStarLevel, activeSkills.size());

        return activeSkills;
    }
}
