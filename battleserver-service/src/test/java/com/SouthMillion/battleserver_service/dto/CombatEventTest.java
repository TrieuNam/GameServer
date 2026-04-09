package com.SouthMillion.battleserver_service.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CombatEvent schema validation
 * Ensures events meet standardized schema requirements
 */
class CombatEventTest {

    @Test
    void testCombatEventSchemaValid() {
        // Create a valid combat event
        CombatEvent event = createValidCombatEvent();

        // Validate required fields are not null
        assertNotNull(event.getEventType(), "eventType must not be null");
        assertNotNull(event.getEventVersion(), "eventVersion must not be null");
        assertNotNull(event.getCombatId(), "combatId must not be null");
        assertNotNull(event.getAttacker(), "attacker must not be null");
        assertNotNull(event.getDefender(), "defender must not be null");
        assertNotNull(event.getResult(), "result must not be null");
        assertNotNull(event.getPerspective(), "perspective must not be null");

        // Validate timestamp is positive
        assertTrue(event.getTimestamp() > 0, "timestamp must be positive");

        // Validate duration is non-negative
        assertTrue(event.getDuration() >= 0, "duration must be non-negative");

        // Validate eventType
        assertEquals("COMBAT_RESULT", event.getEventType(), "eventType should be COMBAT_RESULT");

        // Validate eventVersion
        assertEquals("1.0", event.getEventVersion(), "eventVersion should be 1.0");
    }

    @Test
    void testCombatantDataComplete() {
        CombatEvent.Combatant combatant = createValidCombatant(123L);

        assertNotNull(combatant.getRoleId(), "roleId must not be null");
        assertNotNull(combatant.getName(), "name must not be null");
        assertNotNull(combatant.getLevel(), "level must not be null");
        assertNotNull(combatant.getPower(), "power must not be null");
        assertNotNull(combatant.getDamage(), "damage must not be null");
        assertNotNull(combatant.getDamageTaken(), "damageTaken must not be null");
        assertNotNull(combatant.getHealing(), "healing must not be null");
        assertNotNull(combatant.getFinalHp(), "finalHp must not be null");

        assertTrue(combatant.getRoleId() > 0, "roleId must be positive");
        assertTrue(combatant.getLevel() >= 0, "level must be non-negative");
        assertTrue(combatant.getPower() >= 0, "power must be non-negative");
        assertTrue(combatant.getDamage() >= 0, "damage must be non-negative");
        assertTrue(combatant.getHealing() >= 0, "healing must be non-negative");
    }

    @Test
    void testCombatResultDataComplete() {
        CombatEvent.CombatResult result = createValidCombatResult();

        assertNotNull(result.getWinnerId(), "winnerId must not be null");
        assertNotNull(result.getWinnerSide(), "winnerSide must not be null");
        assertNotNull(result.getTotalRounds(), "totalRounds must not be null");
        assertNotNull(result.getComboMax(), "comboMax must not be null");

        assertTrue(result.getTotalRounds() > 0, "totalRounds must be positive");
        assertTrue(result.getComboMax() >= 0, "comboMax must be non-negative");
        assertTrue(result.getXpGained() >= 0, "xpGained must be non-negative");
        assertTrue(result.getGoldGained() >= 0, "goldGained must be non-negative");

        // Validate winnerSide is valid
        assertTrue(
                result.getWinnerSide().equals("ATTACKER") ||
                result.getWinnerSide().equals("DEFENDER") ||
                result.getWinnerSide().equals("DRAW"),
                "winnerSide must be ATTACKER, DEFENDER, or DRAW"
        );
    }

    @Test
    void testAttackerPerspectiveEvent() {
        CombatEvent event = createValidCombatEvent();
        event.setPerspective("ATTACKER");
        event.setIsWinner(true);

        assertEquals("ATTACKER", event.getPerspective(), "perspective should be ATTACKER");
        assertTrue(event.isWinner(), "attacker should be winner");
    }

    @Test
    void testDefenderPerspectiveEvent() {
        CombatEvent event = createValidCombatEvent();
        event.setPerspective("DEFENDER");
        event.setIsWinner(false);

        assertEquals("DEFENDER", event.getPerspective(), "perspective should be DEFENDER");
        assertFalse(event.isWinner(), "defender should not be winner");
    }

    @Test
    void testCombatTypeValidation() {
        String[] validCombatTypes = {"PVP", "PVE", "ARENA", "TRIAL", "DUNGEON", "BOSS"};

        for (String combatType : validCombatTypes) {
            CombatEvent event = createValidCombatEvent();
            event.setCombatType(combatType);

            assertEquals(combatType, event.getCombatType(), "combatType should match");
        }
    }

    @Test
    void testMetadataPresent() {
        CombatEvent event = createValidCombatEvent();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("server", "battleserver-service");
        metadata.put("version", "1.0.0");
        event.setMetadata(metadata);

        assertNotNull(event.getMetadata(), "metadata should not be null");
        assertEquals("battleserver-service", event.getMetadata().get("server"));
    }

    @Test
    void testDualPerspectiveEventPair() {
        // Create attacker perspective
        CombatEvent attackerEvent = createValidCombatEvent();
        attackerEvent.setPerspective("ATTACKER");
        attackerEvent.setIsWinner(true);

        // Create defender perspective with same combatId
        CombatEvent defenderEvent = createValidCombatEvent();
        defenderEvent.setCombatId(attackerEvent.getCombatId());
        defenderEvent.setPerspective("DEFENDER");
        defenderEvent.setIsWinner(false);

        // Validate they share same combatId
        assertEquals(attackerEvent.getCombatId(), defenderEvent.getCombatId(),
                "Both perspectives should have same combatId");

        // Validate perspectives are different
        assertNotEquals(attackerEvent.getPerspective(), defenderEvent.getPerspective(),
                "Perspectives should be different");

        // Validate winner status matches
        assertTrue(attackerEvent.isWinner(), "Attacker should be winner");
        assertFalse(defenderEvent.isWinner(), "Defender should not be winner");
    }

    @Test
    void testCombatantSurvivalStatus() {
        CombatEvent.Combatant survivor = createValidCombatant(123L);
        survivor.setFinalHp(500);
        survivor.setSurvived(true);

        assertTrue(survivor.getFinalHp() > 0, "Survivor should have HP > 0");
        assertTrue(survivor.isSurvived(), "Survivor flag should be true");

        CombatEvent.Combatant defeated = createValidCombatant(456L);
        defeated.setFinalHp(0);
        defeated.setSurvived(false);

        assertEquals(0, defeated.getFinalHp(), "Defeated should have HP = 0");
        assertFalse(defeated.isSurvived(), "Survived flag should be false");
    }

    @Test
    void testDamageStatisticsConsistency() {
        CombatEvent event = createValidCombatEvent();

        long attackerDamage = event.getAttacker().getDamage();
        long defenderDamageTaken = event.getDefender().getDamageTaken();

        // In a proper event, attacker's damage should match defender's damage taken
        // This is a business logic validation
        assertEquals(attackerDamage, defenderDamageTaken,
                "Attacker's damage should equal defender's damage taken");

        long defenderDamage = event.getDefender().getDamage();
        long attackerDamageTaken = event.getAttacker().getDamageTaken();

        assertEquals(defenderDamage, attackerDamageTaken,
                "Defender's damage should equal attacker's damage taken");
    }

    // Helper methods to create valid test data

    private CombatEvent createValidCombatEvent() {
        CombatEvent.Combatant attacker = createValidCombatant(123L);
        attacker.setDamage(1500L);
        attacker.setDamageTaken(800L);
        attacker.setFinalHp(200);
        attacker.setSurvived(true);

        CombatEvent.Combatant defender = createValidCombatant(456L);
        defender.setDamage(800L);
        defender.setDamageTaken(1500L);
        defender.setFinalHp(0);
        defender.setSurvived(false);

        CombatEvent.CombatResult result = CombatEvent.CombatResult.builder()
                .winnerId(123L)
                .winnerSide("ATTACKER")
                .totalRounds(5)
                .xpGained(100L)
                .goldGained(50L)
                .comboMax(3)
                .build();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("server", "battleserver-service");

        return CombatEvent.builder()
                .eventType("COMBAT_RESULT")
                .eventVersion("1.0")
                .timestamp(System.currentTimeMillis())
                .combatId("test-combat-id-123")
                .sessionId(null)
                .combatType("PVE")
                .duration(1250)
                .attacker(attacker)
                .defender(defender)
                .result(result)
                .metadata(metadata)
                .perspective("ATTACKER")
                .isWinner(true)
                .build();
    }

    private CombatEvent.Combatant createValidCombatant(Long roleId) {
        return CombatEvent.Combatant.builder()
                .roleId(roleId)
                .name("Player" + roleId)
                .level(50)
                .power(5000L)
                .damage(1000L)
                .damageTaken(500L)
                .healing(0L)
                .finalHp(500)
                .survived(true)
                .build();
    }

    private CombatEvent.CombatResult createValidCombatResult() {
        return CombatEvent.CombatResult.builder()
                .winnerId(123L)
                .winnerSide("ATTACKER")
                .totalRounds(5)
                .xpGained(100L)
                .goldGained(50L)
                .comboMax(3)
                .build();
    }
}
