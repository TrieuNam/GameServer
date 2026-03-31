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
        s.setAttack(atk);
        s.setDefense(def);
        s.setCritRate(critRate);
        s.setCritDamage(critDmg);
        s.setSpeed(speed);
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
