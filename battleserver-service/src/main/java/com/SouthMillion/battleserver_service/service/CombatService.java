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
        
        PlayerStats attacker = request.getAttacker();
        PlayerStats defender = request.getDefender();
        
        CombatResult result = new CombatResult();
        result.setAttackerId(request.getAttackerId());
        result.setDefenderId(request.getDefenderId());
        result.setCombatRounds(new ArrayList<>());
        
        // Combat simulation (max 10 rounds)
        int round = 0;
        int attackerHp = attacker.getHp();
        int defenderHp = defender.getHp();
        
        while (attackerHp > 0 && defenderHp > 0 && round < 10) {
            round++;
            
            // Attacker's turn
            CombatRound attackerTurn = calculateDamage(attacker, defender, round, true);
            defenderHp -= attackerTurn.getDamage();
            attackerTurn.setTargetRemainingHp(Math.max(0, defenderHp));
            result.getCombatRounds().add(attackerTurn);
            
            if (defenderHp <= 0) {
                break; // Attacker wins
            }
            
            // Defender's counter-attack
            CombatRound defenderTurn = calculateDamage(defender, attacker, round, false);
            attackerHp -= defenderTurn.getDamage();
            defenderTurn.setTargetRemainingHp(Math.max(0, attackerHp));
            result.getCombatRounds().add(defenderTurn);
            
            if (attackerHp <= 0) {
                break; // Defender wins
            }
        }
        
        // Determine winner
        if (attackerHp > defenderHp) {
            result.setWinnerId(request.getAttackerId());
        } else if (defenderHp > attackerHp) {
            result.setWinnerId(request.getDefenderId());
        } else {
            result.setWinnerId(request.getAttackerId()); // Tie goes to attacker
        }
        
        result.setAttackerFinalHp(Math.max(0, attackerHp));
        result.setDefenderFinalHp(Math.max(0, defenderHp));
        result.setTotalRounds(round);
        result.setDuration(round * 2000L); // 2 seconds per round
        
        log.info("Combat result: Winner={}, Rounds={}", result.getWinnerId(), round);
        return result;
    }

    /**
     * Calculate damage for a single attack
     */
    private CombatRound calculateDamage(PlayerStats attacker, PlayerStats defender, 
                                        int round, boolean isAttacker) {
        CombatRound combatRound = new CombatRound();
        combatRound.setRound(round);
        combatRound.setAttackerId(isAttacker ? attacker.getPlayerId() : defender.getPlayerId());
        
        // Base damage = Attack - Defense
        int baseDamage = attacker.getAttack() - defender.getDefense();
        baseDamage = Math.max(1, baseDamage); // Minimum 1 damage
        
        // Critical hit check
        boolean isCrit = random.nextInt(100) < attacker.getCritRate();
        int finalDamage = baseDamage;
        
        if (isCrit) {
            finalDamage = (int) (baseDamage * (attacker.getCritDamage() / 100.0));
            combatRound.setCritical(true);
        }
        
        // Speed affects dodge chance
        int dodgeChance = Math.max(0, defender.getSpeed() - attacker.getSpeed()) / 10;
        boolean isDodge = random.nextInt(100) < dodgeChance;
        
        if (isDodge) {
            finalDamage = 0;
            combatRound.setDodged(true);
        }
        
        combatRound.setDamage(finalDamage);
        combatRound.setSkillId(isAttacker ? "basic_attack" : "counter_attack");
        
        return combatRound;
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

        CombatRound roundResult = calculateDamage(attacker, defender, round, isAttacker);
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
            && stats.getHp() > 0 
            && stats.getAttack() > 0 
            && stats.getDefense() >= 0;
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
