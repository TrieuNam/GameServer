package com.SouthMillion.battleserver_service.service;

import com.SouthMillion.battleserver_service.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Combat Calculation Service
 * Handles battle mechanics, damage calculations, and combat results
 */
@Slf4j
@Service
public class CombatService {

    private final Random random = new Random();

    /**
     * Calculate combat result between attacker and defender
     * Formula: Damage = Attack * (1 + CritRate/100 * CritDamage/100) - Defense
     */
    public CombatResult calculateCombat(CombatRequest request) {
        log.debug("Calculating combat: {} vs {}", request.getAttackerId(), request.getDefenderId());

        PlayerStats attacker = copyStats(request.getAttacker());
        PlayerStats defender = copyStats(request.getDefender());

        CombatResult result = new CombatResult();
        result.setAttackerId(request.getAttackerId());
        result.setDefenderId(request.getDefenderId());
        result.setCombatRounds(new ArrayList<>());

        attacker.setSpeed(applyMuddySpeedPenalty(attacker, defender));
        defender.setSpeed(applyMuddySpeedPenalty(defender, attacker));

        int round = 0;
        int attackerHp = valueOrZero(attacker.getHp());
        int defenderHp = valueOrZero(defender.getHp());
        boolean attackerStunned = false;
        boolean defenderStunned = false;

        while (attackerHp > 0 && defenderHp > 0 && round < 10) {
            round++;
            attackerHp = applyRejuvenation(attacker, attackerHp, round);
            defenderHp = applyRejuvenation(defender, defenderHp, round);

            boolean attackerActsFirst = valueOrZero(attacker.getSpeed()) >= valueOrZero(defender.getSpeed());

            if (attackerActsFirst) {
                TurnOutcome attackerTurn = performTurn(attacker, defender, attackerHp, defenderHp, round, attackerStunned, "basic_attack");
                attackerHp = attackerTurn.actorHp;
                defenderHp = attackerTurn.targetHp;
                attackerStunned = attackerTurn.actorStunned;
                defenderStunned = attackerTurn.targetStunned;
                result.getCombatRounds().addAll(attackerTurn.rounds);

                if (defenderHp <= 0) {
                    break;
                }

                TurnOutcome defenderTurn = performTurn(defender, attacker, defenderHp, attackerHp, round, defenderStunned, "basic_attack");
                defenderHp = defenderTurn.actorHp;
                attackerHp = defenderTurn.targetHp;
                defenderStunned = defenderTurn.actorStunned;
                attackerStunned = attackerStunned || defenderTurn.targetStunned;
                result.getCombatRounds().addAll(defenderTurn.rounds);

                if (attackerHp <= 0) {
                    break;
                }
            } else {
                TurnOutcome defenderTurn = performTurn(defender, attacker, defenderHp, attackerHp, round, defenderStunned, "basic_attack");
                defenderHp = defenderTurn.actorHp;
                attackerHp = defenderTurn.targetHp;
                defenderStunned = defenderTurn.actorStunned;
                attackerStunned = defenderTurn.targetStunned;
                result.getCombatRounds().addAll(defenderTurn.rounds);

                if (attackerHp <= 0) {
                    break;
                }

                TurnOutcome attackerTurn = performTurn(attacker, defender, attackerHp, defenderHp, round, attackerStunned, "basic_attack");
                attackerHp = attackerTurn.actorHp;
                defenderHp = attackerTurn.targetHp;
                attackerStunned = attackerTurn.actorStunned;
                defenderStunned = defenderStunned || attackerTurn.targetStunned;
                result.getCombatRounds().addAll(attackerTurn.rounds);

                if (defenderHp <= 0) {
                    break;
                }
            }
        }

        if (attackerHp > defenderHp) {
            result.setWinnerId(request.getAttackerId());
        } else if (defenderHp > attackerHp) {
            result.setWinnerId(request.getDefenderId());
        } else {
            result.setWinnerId(request.getAttackerId());
        }

        result.setAttackerFinalHp(Math.max(0, attackerHp));
        result.setDefenderFinalHp(Math.max(0, defenderHp));
        result.setTotalRounds(round);
        result.setDuration(round * 2000L);

        log.info("Combat result: Winner={}, Rounds={}", result.getWinnerId(), round);
        return result;
    }

    /**
     * Calculate damage for a single attack.
     */
    private CombatRound calculateDamage(PlayerStats attacker, PlayerStats defender,
                                        int round, String skillId) {
        CombatRound combatRound = new CombatRound();
        combatRound.setRound(round);
        combatRound.setAttackerId(attacker.getPlayerId());
        combatRound.setCritical(false);
        combatRound.setDodged(false);
        combatRound.setStunned(false);
        combatRound.setHealingDone(0);
        combatRound.setSkillId(skillId);

        int baseDamage = valueOrZero(attacker.getAttack()) - valueOrZero(defender.getDefense());
        baseDamage = Math.max(1, baseDamage);

        int critChance = clampPercent(valueOrZero(attacker.getCritRate()) - valueOrZero(defender.getCriticalImmunity()));
        boolean isCrit = critChance > 0 && random.nextInt(100) < critChance;
        int finalDamage = baseDamage;

        if (isCrit) {
            int critDamagePercent = Math.max(
                    200,
                    valueOrZero(attacker.getCritDamage())
                            + Math.max(0, valueOrZero(attacker.getTyranny()) - valueOrZero(defender.getBenevolence()))
            );
            finalDamage = Math.max(baseDamage, (int) Math.round(baseDamage * (critDamagePercent / 100.0)));
            combatRound.setCritical(true);
        }

        int dodgeChance = clampPercent(
                Math.max(0, valueOrZero(defender.getSpeed()) - valueOrZero(attacker.getSpeed())) / 10
                        + valueOrZero(defender.getEvasion())
                        - valueOrZero(attacker.getEvasionImmunity())
        );
        boolean isDodge = !isCrit && dodgeChance > 0 && random.nextInt(100) < dodgeChance;

        if (isDodge) {
            finalDamage = 0;
            combatRound.setDodged(true);
        }

        if (finalDamage > 0) {
            int lifestealRate = clampPercent(valueOrZero(attacker.getVampiric()) - valueOrZero(defender.getVampiricImmunity()));
            if (lifestealRate > 0) {
                combatRound.setHealingDone(Math.max(0, (int) Math.floor(finalDamage * (lifestealRate / 100.0))));
            }

            int stunChance = clampPercent(valueOrZero(attacker.getStun()) - valueOrZero(defender.getStunImmunity()));
            if (stunChance > 0 && random.nextInt(100) < stunChance) {
                combatRound.setStunned(true);
            }
        }

        combatRound.setDamage(finalDamage);
        return combatRound;
    }

    private TurnOutcome performTurn(PlayerStats actor, PlayerStats target,
                                    int actorHp, int targetHp, int round,
                                    boolean actorStunned, String skillId) {
        if (actorStunned || actorHp <= 0 || targetHp <= 0) {
            return new TurnOutcome(actorHp, targetHp, false, false, List.of());
        }

        List<CombatRound> rounds = new ArrayList<>();
        boolean actorGetsStunnedNext = false;
        boolean targetGetsStunned = false;
        int currentActorHp = actorHp;
        int currentTargetHp = targetHp;

        int totalHits = 1 + resolveComboExtraHits(actor, target);
        for (int hitIndex = 0; hitIndex < totalHits && currentActorHp > 0 && currentTargetHp > 0; hitIndex++) {
            actor.setHp(currentActorHp);
            target.setHp(currentTargetHp);

            CombatRound attackRound = calculateDamage(
                    actor,
                    target,
                    round,
                    hitIndex == 0 ? skillId : "combo_attack"
            );

            currentTargetHp = Math.max(0, currentTargetHp - valueOrZero(attackRound.getDamage()));
            currentActorHp = Math.min(resolveMaxHp(actor, currentActorHp),
                    currentActorHp + valueOrZero(attackRound.getHealingDone()));
            attackRound.setTargetRemainingHp(currentTargetHp);
            rounds.add(attackRound);
            targetGetsStunned = targetGetsStunned || Boolean.TRUE.equals(attackRound.getStunned());

            if (currentTargetHp <= 0) {
                break;
            }

            if (hitIndex == 0 && shouldTriggerCounter(target, actor, attackRound)) {
                target.setHp(currentTargetHp);
                actor.setHp(currentActorHp);

                CombatRound counterRound = calculateDamage(target, actor, round, "counter_attack");
                currentActorHp = Math.max(0, currentActorHp - valueOrZero(counterRound.getDamage()));
                currentTargetHp = Math.min(resolveMaxHp(target, currentTargetHp),
                        currentTargetHp + valueOrZero(counterRound.getHealingDone()));
                counterRound.setTargetRemainingHp(currentActorHp);
                rounds.add(counterRound);
                actorGetsStunnedNext = actorGetsStunnedNext || Boolean.TRUE.equals(counterRound.getStunned());

                if (currentActorHp <= 0) {
                    break;
                }
            }
        }

        return new TurnOutcome(currentActorHp, currentTargetHp, actorGetsStunnedNext, targetGetsStunned, rounds);
    }

    private int resolveComboExtraHits(PlayerStats attacker, PlayerStats defender) {
        int comboChance = clampPercent(valueOrZero(attacker.getCombo()) - valueOrZero(defender.getComboImmunity()));
        if (comboChance <= 0 || random.nextInt(100) >= comboChance) {
            return 0;
        }
        return comboChance >= 100 ? 2 : 1;
    }

    private boolean shouldTriggerCounter(PlayerStats defender, PlayerStats attacker, CombatRound incomingRound) {
        if (incomingRound == null
                || valueOrZero(incomingRound.getDamage()) <= 0
                || Boolean.TRUE.equals(incomingRound.getStunned())) {
            return false;
        }

        int counterChance = clampPercent(valueOrZero(defender.getCounter()) - valueOrZero(attacker.getCounterImmunity()));
        return counterChance > 0 && random.nextInt(100) < counterChance;
    }

    private int applyMuddySpeedPenalty(PlayerStats stats, PlayerStats opponent) {
        int baseSpeed = valueOrZero(stats.getSpeed());
        int muddyRate = clampPercent(valueOrZero(opponent.getMuddy()) - valueOrZero(stats.getInterdiction()));
        if (muddyRate <= 0) {
            return baseSpeed;
        }
        return Math.max(0, (int) Math.round(baseSpeed * (1.0 - muddyRate / 100.0)));
    }

    private int applyRejuvenation(PlayerStats stats, int currentHp, int round) {
        if (stats == null || currentHp <= 0 || round % 5 != 0) {
            return currentHp;
        }

        int rejuvenationRate = clampPercent(valueOrZero(stats.getRejuvenation()));
        if (rejuvenationRate <= 0) {
            return currentHp;
        }

        int heal = Math.max(1, (int) Math.round(resolveMaxHp(stats, currentHp) * (rejuvenationRate / 100.0)));
        return Math.min(resolveMaxHp(stats, currentHp), currentHp + heal);
    }

    private PlayerStats copyStats(PlayerStats stats) {
        if (stats == null) {
            return new PlayerStats();
        }

        return PlayerStats.builder()
                .playerId(stats.getPlayerId())
                .hp(stats.getHp())
                .maxHp(stats.getMaxHp())
                .attack(stats.getAttack())
                .defense(stats.getDefense())
                .speed(stats.getSpeed())
                .critRate(stats.getCritRate())
                .critDamage(stats.getCritDamage())
                .vampiric(stats.getVampiric())
                .vampiricImmunity(stats.getVampiricImmunity())
                .counter(stats.getCounter())
                .counterImmunity(stats.getCounterImmunity())
                .combo(stats.getCombo())
                .comboImmunity(stats.getComboImmunity())
                .evasion(stats.getEvasion())
                .evasionImmunity(stats.getEvasionImmunity())
                .criticalImmunity(stats.getCriticalImmunity())
                .stun(stats.getStun())
                .stunImmunity(stats.getStunImmunity())
                .tyranny(stats.getTyranny())
                .benevolence(stats.getBenevolence())
                .muddy(stats.getMuddy())
                .interdiction(stats.getInterdiction())
                .rejuvenation(stats.getRejuvenation())
                .level(stats.getLevel())
                .fightPower(stats.getFightPower())
                .build();
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int resolveMaxHp(PlayerStats stats, int fallbackHp) {
        if (stats == null || stats.getMaxHp() == null || stats.getMaxHp() <= 0) {
            return Math.max(1, fallbackHp);
        }
        return stats.getMaxHp();
    }

    private static class TurnOutcome {
        private final int actorHp;
        private final int targetHp;
        private final boolean actorStunned;
        private final boolean targetStunned;
        private final List<CombatRound> rounds;

        private TurnOutcome(int actorHp, int targetHp, boolean actorStunned, boolean targetStunned, List<CombatRound> rounds) {
            this.actorHp = actorHp;
            this.targetHp = targetHp;
            this.actorStunned = actorStunned;
            this.targetStunned = targetStunned;
            this.rounds = rounds;
        }
    }

    /**
     * Execute a single combat action and return the combat round
     */
    public CombatRound executeAction(PlayerStats attacker, PlayerStats defender,
                                     int round, boolean isAttacker, int actionType, int skillId) {
        if (actionType == 4) { // DEFEND
            CombatRound defendRound = new CombatRound();
            defendRound.setRound(round);
            defendRound.setAttackerId(isAttacker ? attacker.getPlayerId() : defender.getPlayerId());
            defendRound.setSkillId("defend");
            defendRound.setDamage(0);
            defendRound.setCritical(false);
            defendRound.setDodged(false);
            return defendRound;
        }

        CombatRound roundResult = calculateDamage(attacker, defender, round, "basic_attack");
        if (actionType == 2 && skillId > 0) {
            roundResult.setSkillId("skill_" + skillId);
        } else if (actionType == 3) {
            roundResult.setSkillId("item");
        }
        return roundResult;
    }

    /**
     * Calculate batch combat for multiple battles (PvE scenarios)
     */
    public List<CombatResult> calculateBatchCombat(List<CombatRequest> requests) {
        log.debug("Calculating batch combat for {} battles", requests.size());
        
        List<CombatResult> results = new ArrayList<>();
        for (CombatRequest request : requests) {
            results.add(calculateCombat(request));
        }
        
        return results;
    }

    /**
     * Validate player stats before combat
     */
    public boolean validatePlayerStats(PlayerStats stats) {
        return stats != null
            && valueOrZero(stats.getHp()) > 0
            && valueOrZero(stats.getAttack()) > 0
            && valueOrZero(stats.getDefense()) >= 0;
    }

    /**
     * Calculate combat rewards based on winner's level and fight power
     */
    public CombatReward calculateRewards(CombatResult result, int winnerLevel, int loserLevel) {
        CombatReward reward = new CombatReward();
        reward.setWinnerId(result.getWinnerId());
        
        // Base rewards
        int baseExp = 100 + (loserLevel * 10);
        int baseGold = 50 + (loserLevel * 5);
        
        // Bonus for higher level opponent
        if (loserLevel > winnerLevel) {
            int levelDiff = loserLevel - winnerLevel;
            baseExp += levelDiff * 20;
            baseGold += levelDiff * 10;
        }
        
        reward.setExpGained(baseExp);
        reward.setGoldGained(baseGold);
        
        // Random item drop (10% chance)
        if (random.nextInt(100) < 10) {
            reward.setItemDropped("potion_health_" + (1 + random.nextInt(3)));
        }
        
        return reward;
    }
}
