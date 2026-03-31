package com.SouthMillion.webSocket_server.handler.trial;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.grpc.TrialGrpcClient;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Trial/Dungeon Challenge System Handler - P1b Priority
 * Handles trial entry, completion, rewards, and progression
 * Uses gRPC client for low-latency calls to trial-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrialHandler implements MessageHandler {

    private final TrialGrpcClient trialGrpcClient;
    private final ObjectMapper objectMapper;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    // Trial operation constants
    private static final int OP_GET_DATA = 0;           // Get trial data
    private static final int OP_START_TRIAL = 1;        // Start trial
    private static final int OP_COMPLETE_TRIAL = 2;     // Complete trial
    private static final int OP_FAIL_TRIAL = 3;         // Fail trial
    private static final int OP_ADVANCE_STAGE = 4;      // Advance to next stage
    private static final int OP_CLAIM_REWARD = 5;       // Claim stage reward
    private static final int OP_RESET_PROGRESS = 6;     // Reset trial progress
    private static final int OP_GET_BEST_RECORD = 7;    // Get best record

    @Override
    public int[] interests() {
        // Trial system message IDs
        return new int[]{2200, 2201, 2202};
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            Long roleId = session.getRoleId();
        
            try {
            // Parse operation type from payload
            int operation = parseOperation(payload);
            int trialId = parseTrialId(payload);
            
            log.info("[trial] Operation: roleId={}, msgId={}, operation={}, trialId={}", 
                    roleId, msgId, operation, trialId);

            Object result = null;

            switch (operation) {
                case OP_GET_DATA:
                    result = handleGetData(roleId);
                    break;

                case OP_START_TRIAL:
                    result = handleStartTrial(roleId, trialId);
                    break;

                case OP_COMPLETE_TRIAL:
                    result = handleCompleteTrial(roleId, trialId, payload);
                    break;

                case OP_FAIL_TRIAL:
                    result = handleFailTrial(roleId, trialId);
                    break;

                case OP_ADVANCE_STAGE:
                    result = handleAdvanceStage(roleId, trialId);
                    break;

                case OP_CLAIM_REWARD:
                    int stageId = parseStageId(payload);
                    result = handleClaimReward(roleId, trialId, stageId);
                    break;

                case OP_RESET_PROGRESS:
                    result = handleResetProgress(roleId, trialId);
                    break;

                case OP_GET_BEST_RECORD:
                    result = handleGetBestRecord(roleId, trialId);
                    break;

                default:
                    log.warn("[trial] Unknown operation: {}", operation);
                    result = Map.of("success", false, "error", "Unknown operation");
            }

            // Send response back to client
            sendResponse(session, msgId, result);

        } catch (Exception e) {
            log.error("[trial] Error handling trial operation: roleId={}, msgId={}", roleId, msgId, e);
            sendErrorResponse(session, msgId, "Trial operation failed: " + e.getMessage());
        }
        });    }

    /**
     * OP0: Get all trial data for role
     */
    private Map<String, Object> handleGetData(Long roleId) {
        try {
            log.debug("[trial] Getting trial data for roleId={}", roleId);
            return Map.of(
                    "success", true,
                    "trials", trialGrpcClient.getRoleTrials(roleId)
            );
        } catch (Exception e) {
            log.error("[trial] Error getting trial data", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * OP1: Start a specific trial
     */
    private Map<String, Object> handleStartTrial(Long roleId, Integer trialId) {
        try {
            log.debug("[trial] Starting trial: roleId={}, trialId={}", roleId, trialId);
            Map<String, Object> record = trialGrpcClient.startTrial(roleId, trialId);
            if (record == null) {
                return Map.of("success", false, "error", "Start trial failed");
            }
            return Map.of("success", true, "trialRecord", record);
        } catch (Exception e) {
            log.error("[trial] Error starting trial", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * OP2: Complete a trial with score/stars/time
     */
    private Map<String, Object> handleCompleteTrial(Long roleId, Integer trialId, byte[] payload) {
        try {
            log.debug("[trial] Completing trial: roleId={}, trialId={}", roleId, trialId);

            // Parse score (int32 at offset 3), stars (uint8 at offset 7),
            // completionTime seconds (int32 at offset 8) from binary payload
            Map<String, Object> request = new HashMap<>();
            long score = parseInt32(payload, 3);
            int stars = (payload != null && payload.length > 7) ? (payload[7] & 0xFF) : 0;
            int completionTime = parseInt32(payload, 8);
            // Clamp to valid ranges; fall back to sensible defaults if not provided
            request.put("score", score > 0 ? score : 1000L);
            request.put("stars", (stars >= 1 && stars <= 3) ? stars : 3);
            request.put("completionTime", completionTime > 0 ? completionTime : 120);

            Map<String, Object> record = trialGrpcClient.completeTrial(
                    roleId,
                    trialId,
                    (Long) request.get("score"),
                    (Integer) request.get("stars"),
                    (Integer) request.get("completionTime")
            );

            if (record == null) {
                return Map.of("success", false, "error", "Complete trial failed");
            }
            publishTaskProgress(roleId, taskActionConditionMapping.trialCompleteTaskKey(), "websocket-trial-complete");
            return Map.of("success", true, "trialRecord", record);
        } catch (Exception e) {
            log.error("[trial] Error completing trial", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * OP3: Fail a trial (no reward)
     */
    private Map<String, Object> handleFailTrial(Long roleId, Integer trialId) {
        try {
            log.debug("[trial] Failing trial: roleId={}, trialId={}", roleId, trialId);
            Map<String, Object> record = trialGrpcClient.failTrial(roleId, trialId);
            if (record == null) {
                return Map.of("success", false, "error", "Fail trial failed");
            }
            return Map.of("success", true, "trialRecord", record);
        } catch (Exception e) {
            log.error("[trial] Error failing trial", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * OP4: Advance to next stage
     */
    private Map<String, Object> handleAdvanceStage(Long roleId, Integer trialId) {
        try {
            log.debug("[trial] Advancing stage: roleId={}, trialId={}", roleId, trialId);
            Map<String, Object> record = trialGrpcClient.advanceStage(roleId, trialId);
            if (record == null) {
                return Map.of("success", false, "error", "Advance stage failed");
            }
            return Map.of("success", true, "trialRecord", record);
        } catch (Exception e) {
            log.error("[trial] Error advancing stage", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * OP5: Claim reward for a stage
     */
    private Map<String, Object> handleClaimReward(Long roleId, Integer trialId, Integer stageId) {
        try {
            log.debug("[trial] Claiming reward: roleId={}, trialId={}, stageId={}", roleId, trialId, stageId);
            Map<String, Object> result = trialGrpcClient.claimReward(roleId, trialId, stageId);
            if (result == null) {
                return Map.of("success", false, "error", "Claim reward failed");
            }
            return result;
        } catch (Exception e) {
            log.error("[trial] Error claiming reward", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * OP6: Reset trial progress
     */
    private Map<String, Object> handleResetProgress(Long roleId, Integer trialId) {
        try {
            log.debug("[trial] Resetting progress: roleId={}, trialId={}", roleId, trialId);
            boolean ok = trialGrpcClient.resetDailyAttempts(roleId, trialId);
            return Map.of("success", ok);
        } catch (Exception e) {
            log.error("[trial] Error resetting progress", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * OP7: Get best score/stage record for trial
     */
    private Map<String, Object> handleGetBestRecord(Long roleId, Integer trialId) {
        try {
            log.debug("[trial] Getting best record: roleId={}, trialId={}", roleId, trialId);
            Map<String, Object> record = trialGrpcClient.getTrialRecord(roleId, trialId);
            if (record == null) {
                return Map.of("success", false, "error", "Best record not found");
            }
            return Map.of(
                    "success", true,
                    "bestScore", record.getOrDefault("bestScore", 0L),
                    "bestStage", record.getOrDefault("bestStage", 0),
                    "starsEarned", record.getOrDefault("starsEarned", 0)
            );
        } catch (Exception e) {
            log.error("[trial] Error getting best record", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // Helper methods for parsing payload

    private int parseOperation(byte[] payload) {
        if (payload == null || payload.length < 1) return OP_GET_DATA;
        return payload[0] & 0xFF;
    }

    private int parseTrialId(byte[] payload) {
        if (payload == null || payload.length < 2) return 1;
        return payload[1] & 0xFF;
    }

    private int parseStageId(byte[] payload) {
        if (payload == null || payload.length < 3) return 1;
        return payload[2] & 0xFF;
    }

    /** Read a big-endian int32 from payload at the given byte offset; returns 0 if out of bounds */
    private int parseInt32(byte[] payload, int offset) {
        if (payload == null || payload.length < offset + 4) return 0;
        return ((payload[offset]     & 0xFF) << 24)
             | ((payload[offset + 1] & 0xFF) << 16)
             | ((payload[offset + 2] & 0xFF) << 8)
             |  (payload[offset + 3] & 0xFF);
    }

    // Response msgId for all trial SC messages
    private static final int MSGID_SC_TRIAL = 2210;

    private void sendResponse(PlayerSession session, int msgId, Object result) {
        try {
            log.debug("[trial] Sending response: msgId={}, result={}", msgId, result);
            Object payload = (result != null) ? result : Map.of();
            byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
            Emitters.emit(session, MSGID_SC_TRIAL, jsonBytes);
        } catch (Exception e) {
            log.error("[trial] Error sending response", e);
            try {
                Emitters.emit(session, MSGID_SC_TRIAL, "{}".getBytes());
            } catch (Exception ignored) {}
        }
    }

    private void sendErrorResponse(PlayerSession session, int msgId, String error) {
        log.error("[trial] Sending error response: msgId={}, error={}", msgId, error);
        try {
            byte[] errorJson = objectMapper.writeValueAsBytes(
                    Map.of("success", false, "error", error != null ? error : "Unknown error"));
            Emitters.emit(session, MSGID_SC_TRIAL, errorJson);
        } catch (Exception ignored) {}
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}
