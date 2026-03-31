package com.SouthMillion.webSocket_server.grpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.combat.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * gRPC Client for Combat/Battle Service
 * 
 * Provides combat calculation and battle simulation services
 * Used by: Arena, Trial, Territory, Dungeon handlers
 * 
 * Performance Target: <20ms per combat call
 * Batch operations: 50-100 combats in <500ms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CombatGrpcClient {

    @GrpcClient("battle-service")
    private CombatServiceGrpc.CombatServiceBlockingStub combatServiceStub;

    /**
     * Calculate single combat result
     * 
     * @param attackerRoleId Attacker role ID
     * @param defenderRoleId Defender role ID  
     * @param combatType Combat type (PVP, PVE, ARENA, DUNGEON, TRIAL)
     * @return Combat result with winner, rounds, damage, rewards
     */
    public Map<String, Object> calculateCombat(long attackerRoleId, long defenderRoleId, String combatType) {
        try {
            log.debug("[combat-grpc] Calculate: {} vs {} type={}", attackerRoleId, defenderRoleId, combatType);
            
            CombatRequest request = CombatRequest.newBuilder()
                    .setAttackerRoleId(attackerRoleId)
                    .setDefenderRoleId(defenderRoleId)
                    .setCombatType(combatType != null ? combatType : "PVP")
                    .build();
            
            CombatResponse response = combatServiceStub.calculateCombat(request);
            
            if (response.getStatus().getSuccess()) {
                Map<String, Object> result = new HashMap<>();
                result.put("attackerWins", response.getAttackerWins());
                result.put("winnerId", response.getAttackerWins() ? attackerRoleId : defenderRoleId);
                result.put("rounds", response.getRounds());
                result.put("duration", response.getCombatDurationMs());
                
                // Attacker stats
                Map<String, Object> attackerStats = new HashMap<>();
                attackerStats.put("roleId", response.getAttacker().getRoleId());
                attackerStats.put("damageDealt", response.getAttacker().getDamageDealt());
                attackerStats.put("damageTaken", response.getAttacker().getDamageTaken());
                attackerStats.put("healingDone", response.getAttacker().getHealingDone());
                attackerStats.put("finalHp", response.getAttacker().getFinalHp());
                attackerStats.put("isAlive", response.getAttacker().getIsAlive());
                result.put("attacker", attackerStats);
                
                // Defender stats
                Map<String, Object> defenderStats = new HashMap<>();
                defenderStats.put("roleId", response.getDefender().getRoleId());
                defenderStats.put("damageDealt", response.getDefender().getDamageDealt());
                defenderStats.put("damageTaken", response.getDefender().getDamageTaken());
                defenderStats.put("healingDone", response.getDefender().getHealingDone());
                defenderStats.put("finalHp", response.getDefender().getFinalHp());
                defenderStats.put("isAlive", response.getDefender().getIsAlive());
                result.put("defender", defenderStats);
                
                // Combat log (rounds detail)
                List<Map<String, Object>> combatLog = response.getCombatLogList().stream()
                        .map(round -> {
                            Map<String, Object> roundMap = new HashMap<>();
                            roundMap.put("round", round.getRoundNumber());
                            roundMap.put("attackerId", round.getAttackerId());
                            roundMap.put("skillId", round.getSkillId());
                            roundMap.put("damage", round.getDamage());
                            roundMap.put("isCrit", round.getIsCrit());
                            roundMap.put("effect", round.getEffect());
                            roundMap.put("attackerHp", round.getAttackerHp());
                            roundMap.put("defenderHp", round.getDefenderHp());
                            return roundMap;
                        })
                        .collect(Collectors.toList());
                result.put("combatLog", combatLog);
                
                // Rewards (if any)
                if (response.hasRewards()) {
                    Map<String, Object> rewards = new HashMap<>();
                    rewards.put("exp", response.getRewards().getExp());
                    
                    // Currencies (gold, diamond, etc.)
                    Map<String, Long> currencies = new HashMap<>();
                    response.getRewards().getCurrenciesList().forEach(currency -> {
                        currencies.put(currency.getCurrencyType(), currency.getAmount());
                    });
                    rewards.put("currencies", currencies);
                    
                    // Items
                    rewards.put("items", response.getRewards().getItemsList().stream()
                            .map(item -> {
                                Map<String, Object> itemMap = new HashMap<>();
                                itemMap.put("itemId", item.getItemId());
                                itemMap.put("quantity", item.getQuantity());
                                return itemMap;
                            })
                            .collect(Collectors.toList()));
                    result.put("rewards", rewards);
                }
                
                log.debug("[combat-grpc] Combat result: winner={} rounds={} duration={}ms", 
                        result.get("winnerId"), response.getRounds(), response.getCombatDurationMs());
                
                return result;
            } else {
                log.error("[combat-grpc] Combat failed: {}", response.getStatus().getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("[combat-grpc] Error calculating combat: {} vs {}", attackerRoleId, defenderRoleId, e);
            return null;
        }
    }

    /**
     * Calculate batch combats (for sweep, multi-stage dungeons)
     * 
     * @param combats List of combat pairs (attackerId, defenderId, type)
     * @return List of combat results
     */
    public List<Map<String, Object>> batchCalculateCombat(List<Map<String, Object>> combats) {
        try {
            log.debug("[combat-grpc] Batch calculate: {} combats", combats.size());
            
            List<CombatRequest> requests = combats.stream()
                    .map(combat -> CombatRequest.newBuilder()
                            .setAttackerRoleId(((Number) combat.get("attackerId")).longValue())
                            .setDefenderRoleId(((Number) combat.get("defenderId")).longValue())
                            .setCombatType((String) combat.getOrDefault("type", "PVE"))
                            .build())
                    .collect(Collectors.toList());
            
            BatchCombatRequest batchRequest = BatchCombatRequest.newBuilder()
                    .addAllCombats(requests)
                    .setParallelExecution(true)
                    .build();
            
            BatchCombatResponse batchResponse = combatServiceStub.batchCalculateCombat(batchRequest);
            
            if (batchResponse.getStatus().getSuccess()) {
                List<Map<String, Object>> results = batchResponse.getResultsList().stream()
                        .map(response -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("attackerWins", response.getAttackerWins());
                            result.put("winnerId", response.getAttackerWins() ? 
                                    response.getAttacker().getRoleId() : response.getDefender().getRoleId());
                            result.put("rounds", response.getRounds());
                            result.put("duration", response.getCombatDurationMs());
                            result.put("attackerFinalHp", response.getAttacker().getFinalHp());
                            result.put("defenderFinalHp", response.getDefender().getFinalHp());
                            return result;
                        })
                        .collect(Collectors.toList());
                
                log.debug("[combat-grpc] Batch complete: {} results in {}ms", 
                        results.size(), batchResponse.getTotalProcessingTimeMs());
                
                return results;
            } else {
                log.error("[combat-grpc] Batch combat failed: {}", batchResponse.getStatus().getMessage());
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            log.error("[combat-grpc] Error batch calculating combats", e);
            return Collections.emptyList();
        }
    }

    /**
     * Start real-time combat session (for interactive PvP)
     * 
     * @param attackerIds Attacker team role IDs
     * @param defenderIds Defender team role IDs
     * @param combatType Combat type
     * @return Session ID and start time
     */
    public Map<String, Object> startCombat(List<Long> attackerIds, List<Long> defenderIds, String combatType) {
        try {
            log.debug("[combat-grpc] Start combat: {} vs {} type={}", attackerIds, defenderIds, combatType);
            
            StartCombatRequest request = StartCombatRequest.newBuilder()
                    .addAllAttackerRoleIds(attackerIds)
                    .addAllDefenderRoleIds(defenderIds)
                    .setCombatType(combatType != null ? combatType : "PVP")
                    .build();
            
            CombatSession session = combatServiceStub.startCombat(request);
            
            if (session.getStatus().getSuccess()) {
                Map<String, Object> result = new HashMap<>();
                result.put("sessionId", session.getSessionId());
                result.put("startTime", session.getStartTime());
                
                log.debug("[combat-grpc] Combat session started: {}", session.getSessionId());
                return result;
            } else {
                log.error("[combat-grpc] Start combat failed: {}", session.getStatus().getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("[combat-grpc] Error starting combat", e);
            return null;
        }
    }

    /**
     * Execute combat action in session
     * 
     * @param sessionId Combat session ID
     * @param actorRoleId Actor performing the action
     * @param actionType Action type (1=ATTACK, 2=SKILL, 3=ITEM, 4=DEFEND)
     * @param skillId Skill ID (if action type is SKILL)
     * @param targetRoleId Target role ID
     * @return Action result with round data
     */
    public Map<String, Object> executeAction(String sessionId, long actorRoleId, int actionType, 
                                             int skillId, long targetRoleId) {
        try {
            log.debug("[combat-grpc] Execute action: session={} actor={} action={}", 
                    sessionId, actorRoleId, actionType);
            
            CombatActionRequest request = CombatActionRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setActorRoleId(actorRoleId)
                    .setActionType(actionType)
                    .setSkillId(skillId)
                    .setTargetRoleId(targetRoleId)
                    .build();
            
            CombatActionResponse response = combatServiceStub.executeAction(request);
            
            if (response.getStatus().getSuccess()) {
                Map<String, Object> result = new HashMap<>();
                result.put("sessionId", response.getSessionId());
                result.put("combatEnded", response.getCombatEnded());
                result.put("winnerSide", response.getWinnerSide());
                
                // Round result
                if (response.hasRoundResult()) {
                    Map<String, Object> roundResult = new HashMap<>();
                    roundResult.put("round", response.getRoundResult().getRoundNumber());
                    roundResult.put("attackerId", response.getRoundResult().getAttackerId());
                    roundResult.put("skillId", response.getRoundResult().getSkillId());
                    roundResult.put("damage", response.getRoundResult().getDamage());
                    roundResult.put("isCrit", response.getRoundResult().getIsCrit());
                    roundResult.put("effect", response.getRoundResult().getEffect());
                    roundResult.put("attackerHp", response.getRoundResult().getAttackerHp());
                    roundResult.put("defenderHp", response.getRoundResult().getDefenderHp());
                    result.put("roundResult", roundResult);
                }
                
                return result;
            } else {
                log.error("[combat-grpc] Execute action failed: {}", response.getStatus().getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("[combat-grpc] Error executing action", e);
            return null;
        }
    }

    /**
     * End combat session
     * 
     * @param sessionId Combat session ID
     * @param endReason End reason (VICTORY, DEFEAT, TIMEOUT, DISCONNECT)
     * @return Final combat result
     */
    public Map<String, Object> endCombat(String sessionId, String endReason) {
        try {
            log.debug("[combat-grpc] End combat: session={} reason={}", sessionId, endReason);
            
            EndCombatRequest request = EndCombatRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setEndReason(endReason != null ? endReason : "VICTORY")
                    .build();
            
            CombatResult result = combatServiceStub.endCombat(request);
            
            if (result.getStatus().getSuccess()) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("sessionId", result.getSessionId());
                resultMap.put("attackerWins", result.getAttackerWins());
                resultMap.put("totalRounds", result.getTotalRounds());
                resultMap.put("duration", result.getCombatDurationMs());
                
                log.debug("[combat-grpc] Combat ended: winner={} rounds={}", 
                        result.getAttackerWins(), result.getTotalRounds());
                
                return resultMap;
            } else {
                log.error("[combat-grpc] End combat failed: {}", result.getStatus().getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("[combat-grpc] Error ending combat", e);
            return null;
        }
    }

    /**
     * Quick combat for single target (simplified API for common case)
     * 
     * @param attackerId Attacker role ID
     * @param defenderId Defender role ID
     * @return Winner ID and basic stats
     */
    public Map<String, Object> quickCombat(long attackerId, long defenderId) {
        Map<String, Object> fullResult = calculateCombat(attackerId, defenderId, "PVE");
        
        if (fullResult != null) {
            Map<String, Object> quickResult = new HashMap<>();
            quickResult.put("winnerId", fullResult.get("winnerId"));
            quickResult.put("rounds", fullResult.get("rounds"));
            quickResult.put("duration", fullResult.get("duration"));
            return quickResult;
        }
        
        return null;
    }
}
