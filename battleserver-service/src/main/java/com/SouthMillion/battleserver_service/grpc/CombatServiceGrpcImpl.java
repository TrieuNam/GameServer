package com.SouthMillion.battleserver_service.grpc;

import com.SouthMillion.battleserver_service.dto.*;
import com.SouthMillion.battleserver_service.publisher.CombatEventPublisher;
import com.SouthMillion.battleserver_service.service.CombatService;
import com.SouthMillion.battleserver_service.service.MonsterStatsService;
import com.SouthMillion.battleserver_service.service.RoleStatsService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.combat.*;
import org.SouthMillion.grpc.common.ResponseStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Service Implementation for Combat System
 * Adapts proto messages to internal business logic
 * 
 * Target Performance: <20ms per combat calculation
 * Throughput: >5000 combats/sec
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CombatServiceGrpcImpl extends CombatServiceGrpc.CombatServiceImplBase {

    private final CombatService combatService;
    private final CombatEventPublisher eventPublisher;
    private final RoleStatsService roleStatsService;
    private final MonsterStatsService monsterStatsService;
        private final Map<String, CombatSessionState> sessions = new ConcurrentHashMap<>();
        private final Map<String, CopyOnWriteArrayList<StreamObserver<org.SouthMillion.grpc.combat.CombatEvent>>> streamObservers = new ConcurrentHashMap<>();

    @Override
    public void calculateCombat(org.SouthMillion.grpc.combat.CombatRequest request, 
                                StreamObserver<org.SouthMillion.grpc.combat.CombatResponse> responseObserver) {
        log.debug("gRPC CalculateCombat: {} vs {}", request.getAttackerRoleId(), request.getDefenderRoleId());
        
        try {
            // Convert proto request to internal DTO
            com.SouthMillion.battleserver_service.dto.CombatRequest internalRequest = 
                    convertToInternalRequest(request);
            
            // Validate
            if (!combatService.validatePlayerStats(internalRequest.getAttacker()) ||
                !combatService.validatePlayerStats(internalRequest.getDefender())) {
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Invalid player stats")
                        .asRuntimeException());
                return;
            }
            
            // Calculate combat
            com.SouthMillion.battleserver_service.dto.CombatResult result =
                    combatService.calculateCombat(internalRequest);

            // Publish combat result event to Kafka (legacy format)
            publishCombatEvent(request.getAttackerRoleId(), result, request.getCombatType());

            // Publish dual-perspective events (new standardized format)
            publishDualPerspectiveEvents(result, request.getCombatType());

            // Convert result to proto response
            org.SouthMillion.grpc.combat.CombatResponse response = convertToProtoResponse(result);
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in CalculateCombat: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Combat calculation failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void batchCalculateCombat(org.SouthMillion.grpc.combat.BatchCombatRequest request, 
                                     StreamObserver<org.SouthMillion.grpc.combat.BatchCombatResponse> responseObserver) {
        log.debug("gRPC BatchCalculateCombat for {} battles", request.getCombatsCount());
        
        try {
            List<com.SouthMillion.battleserver_service.dto.CombatRequest> internalRequests = 
                    request.getCombatsList().stream()
                            .map(this::convertToInternalRequest)
                            .collect(Collectors.toList());
            
            List<com.SouthMillion.battleserver_service.dto.CombatResult> results = 
                    combatService.calculateBatchCombat(internalRequests);
            
            org.SouthMillion.grpc.combat.BatchCombatResponse response = 
                    org.SouthMillion.grpc.combat.BatchCombatResponse.newBuilder()
                            .addAllResults(results.stream()
                                    .map(this::convertToProtoResponse)
                                    .collect(Collectors.toList()))
                            .setTotalProcessingTimeMs(results.size() * 20L)
                            .setStatus(ResponseStatus.newBuilder()
                                    .setCode(200)
                                    .setMessage("Success")
                                    .setSuccess(true)
                                    .build())
                            .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in BatchCalculateCombat: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Batch combat failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void startCombat(org.SouthMillion.grpc.combat.StartCombatRequest request,
                           StreamObserver<org.SouthMillion.grpc.combat.CombatSession> responseObserver) {
        log.debug("gRPC StartCombat");
        
        try {
            if (request.getAttackerRoleIdsCount() == 0 || request.getDefenderRoleIdsCount() == 0) {
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Missing attacker or defender role IDs")
                        .asRuntimeException());
                return;
            }

            // Generate session ID
            String sessionId = java.util.UUID.randomUUID().toString();

            Long attackerId = request.getAttackerRoleIds(0);
            Long defenderId = request.getDefenderRoleIds(0);
            CombatSessionState state = new CombatSessionState(sessionId,
                    createCombatantStats(attackerId, request.getContext(), false, request.getCombatType()),
                    createCombatantStats(defenderId, request.getContext(), true, request.getCombatType()),
                    request.getCombatType());
            sessions.put(sessionId, state);
            
            org.SouthMillion.grpc.combat.CombatSession session = 
                    org.SouthMillion.grpc.combat.CombatSession.newBuilder()
                            .setSessionId(sessionId)
                            .setStartTime(System.currentTimeMillis())
                            .setStatus(ResponseStatus.newBuilder()
                                    .setCode(200)
                                    .setMessage("Combat session started")
                                    .setSuccess(true)
                                    .build())
                            .build();
            
            responseObserver.onNext(session);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in StartCombat: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to start combat: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void executeAction(org.SouthMillion.grpc.combat.CombatActionRequest request,
                             StreamObserver<org.SouthMillion.grpc.combat.CombatActionResponse> responseObserver) {
        log.debug("gRPC ExecuteAction for session {}", request.getSessionId());
        
        try {
                        CombatSessionState state = sessions.get(request.getSessionId());
                        if (state == null) {
                                responseObserver.onError(io.grpc.Status.NOT_FOUND
                                                .withDescription("Combat session not found: " + request.getSessionId())
                                                .asRuntimeException());
                                return;
                        }
                        if (state.ended) {
                                responseObserver.onNext(buildActionResponse(request.getSessionId(), null, true, state.getWinnerSide()));
                                responseObserver.onCompleted();
                                return;
                        }

                        boolean actorIsAttacker = request.getActorRoleId() == state.attacker.getPlayerId();
                        PlayerStats actor = actorIsAttacker ? state.attacker : state.defender;
                        PlayerStats target = actorIsAttacker ? state.defender : state.attacker;

                        state.round++;
                        com.SouthMillion.battleserver_service.dto.CombatRound round =
                                        combatService.executeAction(actor, target, state.round, actorIsAttacker,
                                                        request.getActionType(), request.getSkillId());

                        int damage = round.getDamage() != null ? round.getDamage() : 0;
                        int targetHp = Math.max(0, target.getHp() - damage);
                        target.setHp(targetHp);
                        round.setTargetRemainingHp(targetHp);

                        if (actorIsAttacker) {
                                state.attackerDamageDealt += damage;
                                state.defenderDamageTaken += damage;
                        } else {
                                state.defenderDamageDealt += damage;
                                state.attackerDamageTaken += damage;
                        }

                        boolean combatEnded = targetHp <= 0;
                        if (combatEnded) {
                                state.ended = true;
                        }

                        emitCombatEvent(state.sessionId, "ACTION");
            org.SouthMillion.grpc.combat.CombatActionResponse response = 
                                        buildActionResponse(state.sessionId, round, combatEnded, state.getWinnerSide());
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in ExecuteAction: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to execute action: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void endCombat(org.SouthMillion.grpc.combat.EndCombatRequest request,
                         StreamObserver<org.SouthMillion.grpc.combat.CombatResult> responseObserver) {
        log.debug("gRPC EndCombat for session {}", request.getSessionId());
        
        try {
            CombatSessionState state = sessions.remove(request.getSessionId());
            if (state == null) {
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Combat session not found: " + request.getSessionId())
                        .asRuntimeException());
                return;
            }

            state.ended = true;

            org.SouthMillion.grpc.combat.CombatantResult attackerResult =
                    org.SouthMillion.grpc.combat.CombatantResult.newBuilder()
                            .setRoleId(state.attacker.getPlayerId())
                            .setDamageDealt((int) state.attackerDamageDealt)
                            .setDamageTaken((int) state.attackerDamageTaken)
                            .setHealingDone(0)
                            .setFinalHp(state.attacker.getHp())
                            .setIsAlive(state.attacker.getHp() > 0)
                            .build();

            org.SouthMillion.grpc.combat.CombatantResult defenderResult =
                    org.SouthMillion.grpc.combat.CombatantResult.newBuilder()
                            .setRoleId(state.defender.getPlayerId())
                            .setDamageDealt((int) state.defenderDamageDealt)
                            .setDamageTaken((int) state.defenderDamageTaken)
                            .setHealingDone(0)
                            .setFinalHp(state.defender.getHp())
                            .setIsAlive(state.defender.getHp() > 0)
                            .build();

            org.SouthMillion.grpc.combat.CombatResult result =
                    org.SouthMillion.grpc.combat.CombatResult.newBuilder()
                            .setSessionId(request.getSessionId())
                            .setAttackerWins(state.getWinnerSide().equals("ATTACKER"))
                            .setTotalRounds(state.round)
                            .setCombatDurationMs(System.currentTimeMillis() - state.startTimeMs)
                            .addResults(attackerResult)
                            .addResults(defenderResult)
                            .setStatus(ResponseStatus.newBuilder()
                                    .setCode(200)
                                    .setMessage("Combat ended")
                                    .setSuccess(true)
                                    .build())
                            .build();

            // Publish end-of-session combat metrics so session-mode battles are included in analytics.
            com.SouthMillion.battleserver_service.dto.CombatResult internalResult =
                    com.SouthMillion.battleserver_service.dto.CombatResult.builder()
                            .attackerId(state.attacker.getPlayerId())
                            .defenderId(state.defender.getPlayerId())
                            .winnerId("ATTACKER".equals(state.getWinnerSide()) ? state.attacker.getPlayerId() :
                                      ("DEFENDER".equals(state.getWinnerSide()) ? state.defender.getPlayerId() : null))
                            .attackerFinalHp(state.attacker.getHp())
                            .defenderFinalHp(state.defender.getHp())
                            .totalRounds(state.round)
                            .duration(System.currentTimeMillis() - state.startTimeMs)
                            .combatRounds(List.of())
                            .build();
            publishCombatEvent(state.attacker.getPlayerId(), internalResult, state.combatType);

            emitCombatEvent(request.getSessionId(), "COMBAT_END");
            completeCombatStream(request.getSessionId());
            
            responseObserver.onNext(result);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in EndCombat: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to end combat: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void streamCombatEvents(org.SouthMillion.grpc.combat.CombatStreamRequest request,
                                   StreamObserver<org.SouthMillion.grpc.combat.CombatEvent> responseObserver) {
        log.debug("gRPC StreamCombatEvents for session {}", request.getSessionId());
        
        try {
                        streamObservers
                                        .computeIfAbsent(request.getSessionId(), k -> new CopyOnWriteArrayList<>())
                                        .add(responseObserver);

                        org.SouthMillion.grpc.combat.CombatEvent startEvent =
                                        org.SouthMillion.grpc.combat.CombatEvent.newBuilder()
                                                        .setSessionId(request.getSessionId())
                                                        .setTimestamp(System.currentTimeMillis())
                                                        .setEventType("STREAM_START")
                                                        .build();
                        responseObserver.onNext(startEvent);
            
        } catch (Exception e) {
            log.error("Error in StreamCombatEvents: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    // ========== CONVERSION METHODS ==========

    /**
     * Convert proto CombatRequest to internal DTO
     */
    private com.SouthMillion.battleserver_service.dto.CombatRequest convertToInternalRequest(
            org.SouthMillion.grpc.combat.CombatRequest protoRequest) {
        
        PlayerStats attacker = createCombatantStats(
                protoRequest.getAttackerRoleId(),
                protoRequest.getContext(),
                false,
                protoRequest.getCombatType()
        );
        PlayerStats defender = createCombatantStats(
                protoRequest.getDefenderRoleId(),
                protoRequest.getContext(),
                true,
                protoRequest.getCombatType()
        );
        
        return com.SouthMillion.battleserver_service.dto.CombatRequest.builder()
                .attackerId(protoRequest.getAttackerRoleId())
                .defenderId(protoRequest.getDefenderRoleId())
                .attacker(attacker)
                .defender(defender)
                .combatType(protoRequest.getCombatType())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Convert internal CombatResult to proto CombatResponse
     */
    private org.SouthMillion.grpc.combat.CombatResponse convertToProtoResponse(
            com.SouthMillion.battleserver_service.dto.CombatResult internalResult) {
        
        org.SouthMillion.grpc.combat.CombatResponse.Builder responseBuilder = 
                org.SouthMillion.grpc.combat.CombatResponse.newBuilder()
                        .setAttackerWins(internalResult.getWinnerId().equals(internalResult.getAttackerId()))
                        .setRounds(internalResult.getTotalRounds())
                        .setCombatDurationMs(internalResult.getDuration())
                        .setStatus(ResponseStatus.newBuilder()
                                .setCode(200)
                                .setMessage("Success")
                                .setSuccess(true)
                                .build());
        
        // Add attacker result
        responseBuilder.setAttacker(org.SouthMillion.grpc.combat.CombatantResult.newBuilder()
                .setRoleId(internalResult.getAttackerId())
                .setFinalHp(internalResult.getAttackerFinalHp())
                .setIsAlive(internalResult.getAttackerFinalHp() > 0)
                .build());
        
        // Add defender result
        responseBuilder.setDefender(org.SouthMillion.grpc.combat.CombatantResult.newBuilder()
                .setRoleId(internalResult.getDefenderId())
                .setFinalHp(internalResult.getDefenderFinalHp())
                .setIsAlive(internalResult.getDefenderFinalHp() > 0)
                .build());
        
        // Add combat log (rounds)
        if (internalResult.getCombatRounds() != null) {
            internalResult.getCombatRounds().forEach(round -> {
                org.SouthMillion.grpc.combat.CombatRound protoRound = 
                        org.SouthMillion.grpc.combat.CombatRound.newBuilder()
                                .setRoundNumber(round.getRound())
                                .setAttackerId(round.getAttackerId())
                                .setSkillId(parseSkillId(round.getSkillId()))
                                .setDamage(round.getDamage())
                                .setIsCrit(round.getCritical() != null && round.getCritical())
                                .setEffect(round.getDodged() != null && round.getDodged() ? "DODGE" :
                                          (round.getStunned() != null && round.getStunned() ? "STUN" :
                                          (round.getCritical() != null && round.getCritical() ? "CRIT" : "NORMAL")))
                                .setAttackerHp(round.getTargetRemainingHp())
                                .setDefenderHp(round.getTargetRemainingHp())
                                .build();
                responseBuilder.addCombatLog(protoRound);
            });
        }
        
        return responseBuilder.build();
    }

    /**
     * Convert internal CombatResult to proto CombatResult (for EndCombat)
     */
    private org.SouthMillion.grpc.combat.CombatResult convertToProtoResult(
            com.SouthMillion.battleserver_service.dto.CombatResult internalResult) {
        
        return org.SouthMillion.grpc.combat.CombatResult.newBuilder()
                .setSessionId("auto-" + internalResult.getAttackerId())
                .setAttackerWins(internalResult.getWinnerId().equals(internalResult.getAttackerId()))
                .setTotalRounds(internalResult.getTotalRounds())
                .setCombatDurationMs(internalResult.getDuration())
                .setStatus(ResponseStatus.newBuilder()
                        .setCode(200)
                        .setMessage("Success")
                        .setSuccess(true)
                        .build())
                .build();
    }
    
    /**
     * Publish combat result event to Kafka (Legacy single-perspective)
     */
    private void publishCombatEvent(long roleId,
                                    com.SouthMillion.battleserver_service.dto.CombatResult result,
                                    String combatType) {
        try {
            // winnerId is Long in DTO, so compare as numeric values to avoid false negatives.
            boolean isVictory = result.getWinnerId() != null && result.getWinnerId() == roleId;
            Integer duration = result.getDuration() != null ? result.getDuration().intValue() : 0;

                        long totalDamage = 0L;
                        int comboMax = 0;
                        int currentCombo = 0;

                        if (result.getCombatRounds() != null) {
                                for (com.SouthMillion.battleserver_service.dto.CombatRound round : result.getCombatRounds()) {
                                        if (round.getDamage() != null) {
                                                totalDamage += round.getDamage();
                                        }
                                        if (round.getDamage() != null && round.getDamage() > 0) {
                                                currentCombo++;
                                                comboMax = Math.max(comboMax, currentCombo);
                                        } else {
                                                currentCombo = 0;
                                        }
                                }
                        }

                        Integer enemyId = 0;
            if (result.getDefenderId() != null) {
                enemyId = result.getDefenderId().intValue();
            }

                        String enemyType = (combatType != null && combatType.toUpperCase().contains("PVP"))
                                        ? "PLAYER" : "MONSTER";

            eventPublisher.publishCombatResult(
                    Long.valueOf(roleId),
                    (combatType == null || combatType.isEmpty()) ? "PVE" : combatType,
                    isVictory,
                    duration,
                                        enemyType,
                    enemyId,
                    0, // Enemy level not available
                                        totalDamage,
                    0L, // Total healing not tracked
                    isVictory ? 1 : 0, // Kill count
                    isVictory ? 0 : 1, // Death count
                                        comboMax,
                    0L, // EXP calculated separately
                    null  // Items list null
            );

            log.debug("Published combat event for roleId={}, victory={}", roleId, isVictory);
        } catch (Exception e) {
            log.warn("Failed to publish combat event: {}", e.getMessage());
            // Don't fail combat on publish error
        }
    }

    /**
     * Publish dual-perspective combat events (New standardized approach)
     * Publishes events from both attacker and defender viewpoints
     */
    private void publishDualPerspectiveEvents(com.SouthMillion.battleserver_service.dto.CombatResult result,
                                              String combatType) {
        try {
            String combatId = java.util.UUID.randomUUID().toString();
            long timestamp = System.currentTimeMillis();
            int durationMs = result.getDuration() != null ? result.getDuration().intValue() : 0;

            // Calculate statistics from combat rounds
            long attackerDamage = 0L;
            long defenderDamage = 0L;
            int comboMax = 0;
            int currentCombo = 0;

            if (result.getCombatRounds() != null) {
                for (com.SouthMillion.battleserver_service.dto.CombatRound round : result.getCombatRounds()) {
                    if (round.getDamage() != null) {
                        if (round.getAttackerId().equals(result.getAttackerId())) {
                            attackerDamage += round.getDamage();
                        } else {
                            defenderDamage += round.getDamage();
                        }

                        if (round.getDamage() > 0) {
                            currentCombo++;
                            comboMax = Math.max(comboMax, currentCombo);
                        } else {
                            currentCombo = 0;
                        }
                    }
                }
            }

            // Build attacker combatant info
            com.SouthMillion.battleserver_service.dto.CombatEvent.Combatant attacker =
                    com.SouthMillion.battleserver_service.dto.CombatEvent.Combatant.builder()
                            .roleId(result.getAttackerId())
                            .name("Player" + result.getAttackerId()) // Name would come from player service
                            .level(0) // Level would come from player service
                            .power(0L) // Power would come from player service
                            .damage(attackerDamage)
                            .damageTaken(defenderDamage)
                            .healing(0L)
                            .finalHp(result.getAttackerFinalHp())
                            .survived(result.getAttackerFinalHp() > 0)
                            .build();

            // Build defender combatant info
            com.SouthMillion.battleserver_service.dto.CombatEvent.Combatant defender =
                    com.SouthMillion.battleserver_service.dto.CombatEvent.Combatant.builder()
                            .roleId(result.getDefenderId())
                            .name("Player" + result.getDefenderId())
                            .level(0)
                            .power(0L)
                            .damage(defenderDamage)
                            .damageTaken(attackerDamage)
                            .healing(0L)
                            .finalHp(result.getDefenderFinalHp())
                            .survived(result.getDefenderFinalHp() > 0)
                            .build();

            // Determine winner
            Long winnerId = result.getWinnerId();
            String winnerSide = winnerId.equals(result.getAttackerId()) ? "ATTACKER" :
                               (winnerId.equals(result.getDefenderId()) ? "DEFENDER" : "DRAW");

            // Build combat result
            com.SouthMillion.battleserver_service.dto.CombatEvent.CombatResult combatResult =
                    com.SouthMillion.battleserver_service.dto.CombatEvent.CombatResult.builder()
                            .winnerId(winnerId)
                            .winnerSide(winnerSide)
                            .totalRounds(result.getTotalRounds())
                            .xpGained(0L) // XP calculated separately
                            .goldGained(0L) // Gold calculated separately
                            .comboMax(comboMax)
                            .build();

            // Build attacker perspective event
            com.SouthMillion.battleserver_service.dto.CombatEvent attackerEvent =
                    com.SouthMillion.battleserver_service.dto.CombatEvent.builder()
                            .eventType("COMBAT_RESULT")
                            .eventVersion("1.0")
                            .timestamp(timestamp)
                            .combatId(combatId)
                            .sessionId(null)
                            .combatType(combatType != null ? combatType : "PVE")
                            .duration(durationMs)
                            .attacker(attacker)
                            .defender(defender)
                            .result(combatResult)
                            .metadata(Map.of("server", "battleserver-service"))
                            .perspective("ATTACKER")
                            .isWinner(winnerId.equals(result.getAttackerId()))
                            .build();

            // Build defender perspective event
            com.SouthMillion.battleserver_service.dto.CombatEvent defenderEvent =
                    com.SouthMillion.battleserver_service.dto.CombatEvent.builder()
                            .eventType("COMBAT_RESULT")
                            .eventVersion("1.0")
                            .timestamp(timestamp)
                            .combatId(combatId)
                            .sessionId(null)
                            .combatType(combatType != null ? combatType : "PVE")
                            .duration(durationMs)
                            .attacker(attacker)
                            .defender(defender)
                            .result(combatResult)
                            .metadata(Map.of("server", "battleserver-service"))
                            .perspective("DEFENDER")
                            .isWinner(winnerId.equals(result.getDefenderId()))
                            .build();

            // Publish both perspectives
            eventPublisher.publishDualPerspective(
                    combatId,
                    result.getAttackerId(),
                    result.getDefenderId(),
                    attackerEvent,
                    defenderEvent
            );

            log.debug("[Combat] Published dual-perspective events for combatId={}, attacker={}, defender={}",
                    combatId, result.getAttackerId(), result.getDefenderId());

        } catch (Exception e) {
            log.warn("[Combat] Failed to publish dual-perspective events: {}", e.getMessage());
            // Don't fail combat on publish error
        }
    }

        private org.SouthMillion.grpc.combat.CombatActionResponse buildActionResponse(
                        String sessionId,
                        com.SouthMillion.battleserver_service.dto.CombatRound round,
                        boolean combatEnded,
                        String winnerSide) {

                org.SouthMillion.grpc.combat.CombatActionResponse.Builder builder =
                                org.SouthMillion.grpc.combat.CombatActionResponse.newBuilder()
                                                .setSessionId(sessionId)
                                                .setCombatEnded(combatEnded)
                                                .setWinnerSide(winnerSide)
                                                .setStatus(ResponseStatus.newBuilder()
                                                                .setCode(200)
                                                                .setMessage("Action executed")
                                                                .setSuccess(true)
                                                                .build());

                if (round != null) {
                        org.SouthMillion.grpc.combat.CombatRound protoRound =
                                        org.SouthMillion.grpc.combat.CombatRound.newBuilder()
                                                        .setRoundNumber(round.getRound())
                                                        .setAttackerId(round.getAttackerId())
                                                        .setSkillId(parseSkillId(round.getSkillId()))
                                                        .setDamage(round.getDamage() != null ? round.getDamage() : 0)
                                                        .setIsCrit(round.getCritical() != null && round.getCritical())
                                                        .setEffect(round.getDodged() != null && round.getDodged() ? "DODGE" :
                                                                        (round.getStunned() != null && round.getStunned() ? "STUN" :
                                                                        (round.getCritical() != null && round.getCritical() ? "CRIT" : "NORMAL")))
                                                        .setAttackerHp(0)
                                                        .setDefenderHp(round.getTargetRemainingHp() != null ? round.getTargetRemainingHp() : 0)
                                                        .build();
                        builder.setRoundResult(protoRound);
                }

                return builder.build();
        }

        private int parseSkillId(String skillId) {
                if (skillId == null) return 0;
                try {
                        return Integer.parseInt(skillId.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                        return 0;
                }
        }

        private void emitCombatEvent(String sessionId, String eventType) {
                List<StreamObserver<org.SouthMillion.grpc.combat.CombatEvent>> observers =
                                streamObservers.get(sessionId);
                if (observers == null || observers.isEmpty()) return;

                org.SouthMillion.grpc.combat.CombatEvent event =
                                org.SouthMillion.grpc.combat.CombatEvent.newBuilder()
                                                .setSessionId(sessionId)
                                                .setTimestamp(System.currentTimeMillis())
                                                .setEventType(eventType)
                                                .build();

                for (StreamObserver<org.SouthMillion.grpc.combat.CombatEvent> observer : observers) {
                        try {
                                observer.onNext(event);
                        } catch (Exception e) {
                                // Ignore broken observers
                        }
                }
        }

        private void completeCombatStream(String sessionId) {
                List<StreamObserver<org.SouthMillion.grpc.combat.CombatEvent>> observers =
                                streamObservers.remove(sessionId);
                if (observers == null) return;
                for (StreamObserver<org.SouthMillion.grpc.combat.CombatEvent> observer : observers) {
                        try {
                                observer.onCompleted();
                        } catch (Exception e) {
                                // Ignore
                        }
                }
        }

        private PlayerStats createDefaultStats(Long roleId) {
                return createCombatantStats(roleId, null, false, null);
        }

        private PlayerStats createCombatantStats(Long roleId,
                                                 CombatContext context,
                                                 boolean defenderSide,
                                                 String combatType) {
                if (defenderSide && shouldUseMonsterStats(context, combatType)) {
                        return monsterStatsService.getMonsterStats(
                                        roleId,
                                        context.getMonsterId(),
                                        context.getStageId(),
                                        context.getIsBoss());
                }
                return roleStatsService.getPlayerStats(roleId);
        }

        private boolean shouldUseMonsterStats(CombatContext context, String combatType) {
                if (context == null || context.getMonsterId() <= 0) {
                        return false;
                }

                String normalizedType = combatType == null ? "" : combatType.trim().toUpperCase();
                return !normalizedType.contains("PVP") && !normalizedType.contains("ARENA");
        }

        private static class CombatSessionState {
                private final String sessionId;
                private final PlayerStats attacker;
                private final PlayerStats defender;
                private final String combatType;
                private final long startTimeMs;
                private int round;
                private boolean ended;
                private long attackerDamageDealt;
                private long defenderDamageDealt;
                private long attackerDamageTaken;
                private long defenderDamageTaken;

                CombatSessionState(String sessionId, PlayerStats attacker, PlayerStats defender, String combatType) {
                        this.sessionId = sessionId;
                        this.attacker = attacker;
                        this.defender = defender;
                        this.combatType = combatType;
                        this.startTimeMs = System.currentTimeMillis();
                        this.round = 0;
                        this.ended = false;
                }

                String getWinnerSide() {
                        if (attacker.getHp() > defender.getHp()) return "ATTACKER";
                        if (defender.getHp() > attacker.getHp()) return "DEFENDER";
                        return "DRAW";
                }
        }
}

