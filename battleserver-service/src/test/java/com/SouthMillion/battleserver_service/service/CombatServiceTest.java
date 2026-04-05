package com.SouthMillion.battleserver_service.service;

import com.SouthMillion.battleserver_service.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CombatService Tests")
class CombatServiceTest {

    // CombatService has no Spring dependencies – instantiate directly
    private final CombatService combatService = new CombatService();

    private static PlayerStats stats(Long id, int hp, int atk, int def, int critRate,
                                     int critDmg, int speed) {
        PlayerStats s = new PlayerStats();
        s.setPlayerId(id);
        s.setHp(hp);
        s.setMaxHp(hp);
        s.setAttack(atk);
        s.setDefense(def);
        s.setCritRate(critRate);
        s.setCritDamage(critDmg);
        s.setSpeed(speed);
        s.setVampiric(0);
        s.setVampiricImmunity(0);
        s.setCounter(0);
        s.setCounterImmunity(0);
        s.setCombo(0);
        s.setComboImmunity(0);
        s.setEvasion(0);
        s.setEvasionImmunity(0);
        s.setCriticalImmunity(0);
        s.setStun(0);
        s.setStunImmunity(0);
        s.setTyranny(0);
        s.setBenevolence(0);
        s.setMuddy(0);
        s.setInterdiction(0);
        s.setRejuvenation(0);
        return s;
    }

    // =========================================================
    // validatePlayerStats
    // =========================================================
    @Nested
    @DisplayName("validatePlayerStats()")
    class ValidateStats {

        @Test
        @DisplayName("TC-COMBAT-001 [P] Stats hop le – tra ve true")
        void validate_validStats_returnsTrue() {
            PlayerStats s = stats(1L, 1000, 200, 50, 10, 150, 100);
            assertThat(combatService.validatePlayerStats(s)).isTrue();
        }

        @Test
        @DisplayName("TC-COMBAT-002 [N] HP bang 0 – tra ve false")
        void validate_zeroHp_returnsFalse() {
            PlayerStats s = stats(1L, 0, 200, 50, 10, 150, 100);
            assertThat(combatService.validatePlayerStats(s)).isFalse();
        }

        @Test
        @DisplayName("TC-COMBAT-003 [N] Attack bang 0 – tra ve false")
        void validate_zeroAttack_returnsFalse() {
            PlayerStats s = stats(1L, 1000, 0, 50, 10, 150, 100);
            assertThat(combatService.validatePlayerStats(s)).isFalse();
        }

        @Test
        @DisplayName("TC-COMBAT-004 [N] Stats null – tra ve false")
        void validate_null_returnsFalse() {
            assertThat(combatService.validatePlayerStats(null)).isFalse();
        }
    }

    // =========================================================
    // calculateCombat
    // =========================================================
    @Nested
    @DisplayName("calculateCombat()")
    class CalculateCombat {

        @Test
        @DisplayName("TC-COMBAT-005 [P] Ke tan cong manh hon – ke tan cong thang")
        void calculateCombat_strongAttacker_attackerWins() {
            // Attacker kills defender in 1 hit: 999 damage vs 1 hp
            PlayerStats attacker = stats(2L, 1000, 999, 0, 0, 150, 0);
            PlayerStats defender = stats(3L, 1, 10, 0, 0, 150, 0);

            CombatRequest req = new CombatRequest();
            req.setAttackerId(2L);
            req.setDefenderId(3L);
            req.setAttacker(attacker);
            req.setDefender(defender);

            CombatResult result = combatService.calculateCombat(req);

            assertThat(result.getWinnerId()).isEqualTo(2L);
            assertThat(result.getDefenderFinalHp()).isEqualTo(0);
            assertThat(result.getTotalRounds()).isGreaterThan(0);
        }

        @Test
        @DisplayName("TC-COMBAT-006 [P] Ket qua chien dau tra ve day du thong tin")
        void calculateCombat_returnsCompleteResult() {
            PlayerStats attacker = stats(4L, 500, 100, 0, 0, 150, 10);
            PlayerStats defender  = stats(5L, 300, 80, 10, 0, 150, 5);

            CombatRequest req = new CombatRequest();
            req.setAttackerId(4L);
            req.setDefenderId(5L);
            req.setAttacker(attacker);
            req.setDefender(defender);

            CombatResult result = combatService.calculateCombat(req);

            assertThat(result.getAttackerId()).isEqualTo(4L);
            assertThat(result.getDefenderId()).isEqualTo(5L);
            assertThat(result.getWinnerId()).isNotNull();
            assertThat(result.getCombatRounds()).isNotEmpty();
            assertThat(result.getTotalRounds()).isLessThanOrEqualTo(10);
            assertThat(result.getDuration()).isGreaterThan(0);
        }

        @Test
        @DisplayName("TC-COMBAT-007 [P] Toi da 10 vong chien dau")
        void calculateCombat_maxRounds_stopsTen() {
            // Both players very tough – will go full 10 rounds
            PlayerStats attacker = stats(6L, 100000, 1, 1000, 0, 150, 0);
            PlayerStats defender  = stats(7L, 100000, 1, 1000, 0, 150, 0);

            CombatRequest req = new CombatRequest();
            req.setAttackerId(6L);
            req.setDefenderId(7L);
            req.setAttacker(attacker);
            req.setDefender(defender);

            CombatResult result = combatService.calculateCombat(req);

            assertThat(result.getTotalRounds()).isLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("TC-COMBAT-011 [P] Evasion attr cao – doi thu ne don danh")
        void calculateCombat_highEvasion_marksDodgedRound() {
            PlayerStats attacker = stats(10L, 500, 120, 10, 0, 200, 0);
            PlayerStats defender = stats(11L, 500, 50, 10, 0, 200, 0);
            defender.setEvasion(100);

            CombatRequest req = new CombatRequest();
            req.setAttackerId(10L);
            req.setDefenderId(11L);
            req.setAttacker(attacker);
            req.setDefender(defender);

            CombatResult result = combatService.calculateCombat(req);

            assertThat(result.getCombatRounds())
                    .anySatisfy(round -> {
                        assertThat(round.getDodged()).isTrue();
                        assertThat(round.getDamage()).isZero();
                    });
        }

        @Test
        @DisplayName("TC-COMBAT-012 [P] Vampiric attr – hoi mau theo sat thuong")
        void calculateCombat_vampiricAttacker_healsAfterDamage() {
            PlayerStats attacker = stats(12L, 400, 300, 0, 0, 200, 0);
            attacker.setMaxHp(1000);
            attacker.setVampiric(100);
            PlayerStats defender = stats(13L, 200, 10, 0, 0, 200, 0);

            CombatRequest req = new CombatRequest();
            req.setAttackerId(12L);
            req.setDefenderId(13L);
            req.setAttacker(attacker);
            req.setDefender(defender);

            CombatResult result = combatService.calculateCombat(req);

            assertThat(result.getAttackerFinalHp()).isGreaterThan(400);
        }

        @Test
        @DisplayName("TC-COMBAT-013 [P] Stun attr – chan don phan cong ngay lap tuc")
        void calculateCombat_stunSkipsImmediateCounter() {
            PlayerStats attacker = stats(14L, 1000, 60, 0, 0, 200, 0);
            attacker.setStun(100);
            PlayerStats defender = stats(15L, 100, 500, 0, 0, 200, 0);

            CombatRequest req = new CombatRequest();
            req.setAttackerId(14L);
            req.setDefenderId(15L);
            req.setAttacker(attacker);
            req.setDefender(defender);

            CombatResult result = combatService.calculateCombat(req);

            assertThat(result.getAttackerFinalHp()).isEqualTo(1000);
        }

        @Test
        @DisplayName("TC-COMBAT-014 [P] Combo attr cao – ha muc tieu trong mot vong")
        void calculateCombat_comboChainsFinishInOneRound() {
            PlayerStats attacker = stats(16L, 1000, 100, 0, 0, 200, 50);
            PlayerStats defender = stats(17L, 250, 10, 0, 0, 200, 10);

            CombatRequest noComboReq = new CombatRequest();
            noComboReq.setAttackerId(16L);
            noComboReq.setDefenderId(17L);
            noComboReq.setAttacker(attacker);
            noComboReq.setDefender(defender);

            CombatResult withoutCombo = combatService.calculateCombat(noComboReq);

            attacker.setCombo(100);
            CombatRequest comboReq = new CombatRequest();
            comboReq.setAttackerId(16L);
            comboReq.setDefenderId(17L);
            comboReq.setAttacker(attacker);
            comboReq.setDefender(stats(17L, 250, 10, 0, 0, 200, 10));

            CombatResult withCombo = combatService.calculateCombat(comboReq);

            assertThat(withoutCombo.getTotalRounds()).isGreaterThan(1);
            assertThat(withCombo.getTotalRounds()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-COMBAT-015 [P] Counter attr chi xuat hien khi co phan cong that")
        void calculateCombat_counterOnlyAppearsWhenTriggered() {
            PlayerStats attacker = stats(18L, 300, 60, 0, 0, 200, 20);
            PlayerStats defender = stats(19L, 500, 40, 0, 0, 200, 20);

            CombatRequest noCounterReq = new CombatRequest();
            noCounterReq.setAttackerId(18L);
            noCounterReq.setDefenderId(19L);
            noCounterReq.setAttacker(attacker);
            noCounterReq.setDefender(defender);

            CombatResult withoutCounter = combatService.calculateCombat(noCounterReq);

            defender.setCounter(100);
            CombatRequest counterReq = new CombatRequest();
            counterReq.setAttackerId(18L);
            counterReq.setDefenderId(19L);
            counterReq.setAttacker(stats(18L, 300, 60, 0, 0, 200, 20));
            counterReq.setDefender(defender);

            CombatResult withCounter = combatService.calculateCombat(counterReq);

            assertThat(withoutCounter.getCombatRounds())
                    .noneMatch(round -> "counter_attack".equals(round.getSkillId()));
            assertThat(withCounter.getCombatRounds())
                    .anyMatch(round -> "counter_attack".equals(round.getSkillId()));
        }

        @Test
        @DisplayName("TC-COMBAT-016 [P] Rejuvenation attr – hoi mau vao vong 5")
        void calculateCombat_rejuvenationHealsOnRoundFive() {
            PlayerStats attacker = stats(20L, 1000, 50, 0, 0, 200, 50);
            attacker.setRejuvenation(50);
            PlayerStats defender = stats(21L, 1000, 50, 0, 0, 200, 50);

            CombatRequest req = new CombatRequest();
            req.setAttackerId(20L);
            req.setDefenderId(21L);
            req.setAttacker(attacker);
            req.setDefender(defender);

            CombatResult result = combatService.calculateCombat(req);

            assertThat(result.getAttackerFinalHp()).isGreaterThan(500);
        }

        @Test
        @DisplayName("TC-COMBAT-017 [P] Muddy attr – giam toc doi thu va doi ket qua")
        void calculateCombat_muddyChangesFastTargetOutcome() {
            PlayerStats attacker = stats(22L, 100, 120, 0, 0, 200, 50);
            PlayerStats defender = stats(23L, 90, 200, 0, 0, 200, 300);

            CombatRequest noMuddyReq = new CombatRequest();
            noMuddyReq.setAttackerId(22L);
            noMuddyReq.setDefenderId(23L);
            noMuddyReq.setAttacker(attacker);
            noMuddyReq.setDefender(defender);
            CombatResult withoutMuddy = combatService.calculateCombat(noMuddyReq);

            attacker.setMuddy(100);
            CombatRequest muddyReq = new CombatRequest();
            muddyReq.setAttackerId(22L);
            muddyReq.setDefenderId(23L);
            muddyReq.setAttacker(attacker);
            muddyReq.setDefender(stats(23L, 90, 200, 0, 0, 200, 300));
            CombatResult withMuddy = combatService.calculateCombat(muddyReq);

            assertThat(withoutMuddy.getWinnerId()).isEqualTo(23L);
            assertThat(withMuddy.getWinnerId()).isEqualTo(22L);
        }
    }

    // =========================================================
    // calculateBatchCombat
    // =========================================================
    @Nested
    @DisplayName("calculateBatchCombat()")
    class CalculateBatchCombat {

        @Test
        @DisplayName("TC-COMBAT-008 [P] Tinh ket qua cho nhieu tran chien")
        void calculateBatchCombat_multipleRequests_returnsAllResults() {
            PlayerStats s1 = stats(4L, 100, 50, 10, 0, 150, 0);
            PlayerStats s2 = stats(5L, 80, 40, 5, 0, 150, 0);

            CombatRequest req1 = new CombatRequest();
            req1.setAttackerId(4L); req1.setDefenderId(5L);
            req1.setAttacker(s1); req1.setDefender(s2);

            PlayerStats s3 = stats(8L, 200, 80, 20, 0, 150, 0);
            PlayerStats s4 = stats(9L, 150, 60, 15, 0, 150, 0);

            CombatRequest req2 = new CombatRequest();
            req2.setAttackerId(8L); req2.setDefenderId(9L);
            req2.setAttacker(s3); req2.setDefender(s4);

            List<CombatResult> results = combatService.calculateBatchCombat(List.of(req1, req2));

            assertThat(results).hasSize(2);
        }
    }

    // =========================================================
    // calculateRewards
    // =========================================================
    @Nested
    @DisplayName("calculateRewards()")
    class CalculateRewards {

        @Test
        @DisplayName("TC-COMBAT-009 [P] Tinh phan thuong khi thang nguoi cung cap – tang thuong")
        void calculateRewards_higherLevelOpponent_bonusRewards() {
            CombatResult result = new CombatResult();
            result.setWinnerId(99L);

            CombatReward reward = combatService.calculateRewards(result, 10, 20);

            // Base: 100 + 20*10 = 300 exp, 50 + 20*5 = 150 gold
            // Bonus: +10*20=200 exp, +10*10=100 gold
            assertThat(reward.getExpGained()).isGreaterThan(300);
            assertThat(reward.getGoldGained()).isGreaterThan(150);
            assertThat(reward.getWinnerId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("TC-COMBAT-010 [P] Tinh phan thuong can ban khi cung cap")
        void calculateRewards_sameLevel_baseRewards() {
            CombatResult result = new CombatResult();
            result.setWinnerId(99L);

            CombatReward reward = combatService.calculateRewards(result, 10, 10);

            // Base: 100 + 10*10 = 200 exp, 50 + 10*5 = 100 gold (no level diff bonus)
            assertThat(reward.getExpGained()).isEqualTo(200);
            assertThat(reward.getGoldGained()).isEqualTo(100);
        }
    }
}
