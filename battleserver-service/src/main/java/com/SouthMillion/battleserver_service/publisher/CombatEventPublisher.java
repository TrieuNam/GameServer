package com.SouthMillion.battleserver_service.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.combat.CombatResultEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Kafka Event Publisher for Combat System
 * Publishes combat results to Kafka for:
 * - Statistics tracking (damage, healing, kills)
 * - Achievement unlocking (combo milestones, kill counts)
 * - Quest progress (kill X enemies)
 * - Analytics and anti-cheat
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CombatEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_COMBAT_RESULT = "combat.result";

    /**
     * Publish combat result event
     * @param roleId Player role ID
     * @param combatType Combat type (PVE, PVP, TRIAL, ARENA, etc.)
     * @param isVictory Did player win?
     * @param duration Combat duration in seconds
     * @param enemyType Enemy type (MONSTER, BOSS, PLAYER)
     * @param enemyId Enemy ID
     * @param enemyLevel Enemy level
     * @param totalDamage Total damage dealt
     * @param totalHealing Total healing done
     * @param killCount Number of kills
     * @param deathCount Number of deaths
     * @param comboMax Maximum combo achieved
     * @param expGained Experience gained
     * @param itemsDropped Items dropped count
     */
    public void publishCombatResult(
            Long roleId,
            String combatType,
            boolean isVictory,
            Integer duration,
            String enemyType,
            Integer enemyId,
            Integer enemyLevel,
            Long totalDamage,
            Long totalHealing,
            Integer killCount,
            Integer deathCount,
            Integer comboMax,
            Long expGained,
            List<org.SouthMillion.dto.event.combat.CombatResultEvent.ItemDrop> itemsDropped
    ) {
        try {
            String eventId = UUID.randomUUID().toString();
            Long combatId = System.currentTimeMillis(); // Use timestamp as combatId
            
            CombatResultEvent event = CombatResultEvent.builder()
                    .eventId(eventId)
                    .combatId(combatId)
                    .roleId(roleId)
                    .combatType(combatType)
                    .isVictory(isVictory)
                    .duration(duration)
                    .enemyType(enemyType)
                    .enemyId(enemyId)
                    .enemyLevel(enemyLevel)
                    .totalDamage(totalDamage)
                    .totalHealing(totalHealing)
                    .killCount(killCount)
                    .deathCount(deathCount)
                    .comboMax(comboMax)
                    .expGained(expGained)
                    .itemsDropped(itemsDropped)
                    .completedAt(java.time.Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC_COMBAT_RESULT, String.valueOf(roleId), event);
            
            log.info("Published combat result event: eventId={}, roleId={}, combatType={}, victory={}, damage={}", 
                    eventId, roleId, combatType, isVictory, totalDamage);
                    
        } catch (Exception e) {
            log.error("Failed to publish combat result event for roleId={}: {}", roleId, e.getMessage(), e);
            // Don't throw - publishing failure shouldn't break combat flow
        }
    }

    /**
     * Convenience method for simple PVE combat results
     */
    public void publishPvECombatResult(Long roleId, boolean isVictory, 
                                       Integer monsterId, Integer monsterLevel,
                                       Long totalDamage, Integer killCount, Long expGained) {
        publishCombatResult(
                roleId,
                "PVE",
                isVictory,
                0, // Duration not tracked for simple PVE
                "MONSTER",
                monsterId,
                monsterLevel,
                totalDamage,
                0L, // No healing tracking
                killCount,
                0, // No death count in PVE
                0, // No combo tracking
                expGained,
                null  // No item tracking
        );
    }

    /**
     * Convenience method for boss combat
     */
    public void publishBossCombatResult(Long roleId, boolean isVictory,
                                        Integer bossId, Integer bossLevel,
                                        Long totalDamage, Long totalHealing,
                                        Integer duration, Integer comboMax) {
        publishCombatResult(
                roleId,
                "BOSS",
                isVictory,
                duration,
                "BOSS",
                bossId,
                bossLevel,
                totalDamage,
                totalHealing,
                isVictory ? 1 : 0, // Boss killed
                isVictory ? 0 : 1, // Player died
                comboMax,
                0L, // EXP calculated separately
                null  // Items calculated separately
        );
    }
}
