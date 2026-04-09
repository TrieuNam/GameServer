package com.SouthMillion.webSocket_server.handler.battle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.grpc.BattleServerGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.grpc.combat.CombatActionResponse;
import org.SouthMillion.grpc.combat.CombatResponse;
import org.SouthMillion.grpc.combat.CombatResult;
import org.SouthMillion.grpc.combat.CombatSession;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Battle handler for realtime combat requests routed through battleserver gRPC.
 *
 * Error Codes:
 * - 1000: INVALID_TARGET
 * - 1001: INSUFFICIENT_STAMINA
 * - 1002: PLAYER_IN_COMBAT
 * - 1003: INVALID_COMBAT_TYPE
 * - 1004: SESSION_NOT_FOUND
 * - 1005: INVALID_ACTION
 * - 1006: INVALID_SKILL
 * - 1007: TARGET_REQUIRED
 * - 1008: SESSION_ENDED
 * - 1009: NOT_YOUR_TURN
 * - 1010: COOLDOWN_ACTIVE
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleHandler implements MessageHandler {

    private static final int OP_CALCULATE_COMBAT = 1;
    private static final int OP_START_SESSION = 2;
    private static final int OP_EXECUTE_ACTION = 3;
    private static final int OP_END_SESSION = 4;

    // Error codes
    private static final int ERROR_INVALID_TARGET = 1000;
    private static final int ERROR_INSUFFICIENT_STAMINA = 1001;
    private static final int ERROR_PLAYER_IN_COMBAT = 1002;
    private static final int ERROR_INVALID_COMBAT_TYPE = 1003;
    private static final int ERROR_SESSION_NOT_FOUND = 1004;
    private static final int ERROR_INVALID_ACTION = 1005;
    private static final int ERROR_INVALID_SKILL = 1006;
    private static final int ERROR_TARGET_REQUIRED = 1007;
    private static final int ERROR_SESSION_ENDED = 1008;
    private static final int ERROR_NOT_YOUR_TURN = 1009;
    private static final int ERROR_COOLDOWN_ACTIVE = 1010;

    private final BattleServerGrpcClient battleServerGrpcClient;
    private final ObjectMapper objectMapper;

    @Override
    public int[] interests() {
        return new int[]{MessageIds.CS_BATTLE_REQ};
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            Long roleId = session.getRoleId();
            if (roleId == null) {
                log.warn("[battle] Reject request: roleId is null");
                return;
            }

            try {
                Map<String, Object> req = parsePayload(payload);
                int op = getInt(req, "op", OP_CALCULATE_COMBAT);
                Map<String, Object> resp;

                switch (op) {
                    case OP_CALCULATE_COMBAT:
                        resp = handleCalculateCombat(roleId, req);
                        break;
                    case OP_START_SESSION:
                        resp = handleStartSession(roleId, req);
                        break;
                    case OP_EXECUTE_ACTION:
                        resp = handleExecuteAction(roleId, req);
                        break;
                    case OP_END_SESSION:
                        resp = handleEndSession(req);
                        break;
                    default:
                        resp = error(ERROR_INVALID_ACTION, "UNSUPPORTED_OP", "Unsupported battle operation: " + op);
                }

                byte[] json = objectMapper.writeValueAsBytes(resp);
                Emitters.emit(session, MessageIds.SC_BATTLE_RESP, json);
            } catch (Exception e) {
                log.error("[battle] Failed to handle message", e);
                safeEmitError(session, "Battle handler failed: " + e.getMessage());
            }
        });
    }

    private Map<String, Object> handleCalculateCombat(Long roleId, Map<String, Object> req) {
        long targetRoleId = getLong(req, "targetRoleId", 0L);
        if (targetRoleId <= 0) {
            return error(ERROR_INVALID_TARGET, "INVALID_TARGET", "targetRoleId is required");
        }

        String combatType = resolveCombatType(getInt(req, "combatType", 1), req);
        BattleServerGrpcClient.CombatContextInfo context = buildContext(req);

        CombatResponse response = battleServerGrpcClient.calculateCombat(
                String.valueOf(roleId),
                String.valueOf(targetRoleId),
                combatType,
                context
        );

        if (response == null) {
            return error(ERROR_INVALID_TARGET, "COMBAT_FAILED", "calculateCombat returned null response");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("attackerWins", response.getAttackerWins());
        data.put("rounds", response.getRounds());
        data.put("durationMs", response.getCombatDurationMs());
        data.put("combatType", combatType);

        return ok(data);
    }

    private Map<String, Object> handleStartSession(Long roleId, Map<String, Object> req) {
        List<String> attackerRoleIds = getStringList(req, "attackerRoleIds");
        if (attackerRoleIds.isEmpty()) {
            attackerRoleIds = List.of(String.valueOf(roleId));
        }

        List<String> defenderRoleIds = getStringList(req, "defenderRoleIds");
        if (defenderRoleIds.isEmpty()) {
            long targetRoleId = getLong(req, "targetRoleId", 0L);
            if (targetRoleId > 0) {
                defenderRoleIds = List.of(String.valueOf(targetRoleId));
            }
        }

        if (defenderRoleIds.isEmpty()) {
            return error(ERROR_TARGET_REQUIRED, "TARGET_REQUIRED", "defenderRoleIds or targetRoleId is required");
        }

        String combatType = resolveCombatType(getInt(req, "combatType", 1), req);
        BattleServerGrpcClient.CombatContextInfo context = buildContext(req);

        CombatSession session = battleServerGrpcClient.startCombat(
                attackerRoleIds,
                defenderRoleIds,
                combatType,
                context
        );

        if (session == null || session.getSessionId() == null || session.getSessionId().isBlank()) {
            return error(ERROR_PLAYER_IN_COMBAT, "SESSION_START_FAILED", "startCombat failed");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getSessionId());
        data.put("startTime", session.getStartTime());
        data.put("combatType", combatType);
        return ok(data);
    }

    private Map<String, Object> handleExecuteAction(Long roleId, Map<String, Object> req) {
        String sessionId = getString(req, "sessionId", "");
        if (sessionId.isBlank()) {
            return error(ERROR_SESSION_NOT_FOUND, "SESSION_ID_REQUIRED", "sessionId is required");
        }

        String actorRoleId = getString(req, "actorRoleId", String.valueOf(roleId));
        int actionType = getInt(req, "actionType", 1);
        int skillId = getInt(req, "skillId", 0);
        String targetRoleId = getString(req, "targetRoleId", "0");

        CombatActionResponse response = battleServerGrpcClient.executeCombatAction(
                sessionId,
                actorRoleId,
                actionType,
                skillId,
                targetRoleId
        );

        if (response == null) {
            return error(ERROR_INVALID_ACTION, "ACTION_FAILED", "executeCombatAction failed");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", response.getSessionId());
        data.put("combatEnded", response.getCombatEnded());
        data.put("winnerSide", response.getWinnerSide());
        data.put("statusCode", response.getStatus().getCode());
        data.put("statusMessage", response.getStatus().getMessage());
        return ok(data);
    }

    private Map<String, Object> handleEndSession(Map<String, Object> req) {
        String sessionId = getString(req, "sessionId", "");
        if (sessionId.isBlank()) {
            return error(ERROR_SESSION_NOT_FOUND, "SESSION_ID_REQUIRED", "sessionId is required");
        }

        String endReason = getString(req, "endReason", "NORMAL_END");
        CombatResult result = battleServerGrpcClient.endCombat(sessionId, endReason);

        if (result == null) {
            return error(ERROR_SESSION_NOT_FOUND, "END_FAILED", "endCombat failed");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", result.getSessionId());
        data.put("attackerWins", result.getAttackerWins());
        data.put("totalRounds", result.getTotalRounds());
        data.put("durationMs", result.getCombatDurationMs());
        data.put("statusCode", result.getStatus().getCode());
        data.put("statusMessage", result.getStatus().getMessage());
        return ok(data);
    }

    private BattleServerGrpcClient.CombatContextInfo buildContext(Map<String, Object> req) {
        BattleServerGrpcClient.CombatContextInfo context = new BattleServerGrpcClient.CombatContextInfo();
        int stageId = getInt(req, "stageId", 0);
        int monsterId = getInt(req, "monsterId", 0);
        if (stageId > 0) context.setStageId(stageId);
        if (monsterId > 0) context.setMonsterId(monsterId);
        context.setBoss(getBoolean(req, "isBoss", false));
        return context;
    }

    private Map<String, Object> parsePayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return Map.of();
        }

        // Prefer JSON payload when client sends structured data.
        if (payload[0] == '{') {
            try {
                return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                // Fallback to binary parser below.
            }
        }

        Map<String, Object> req = new HashMap<>();
        ByteBuffer buf = ByteBuffer.wrap(payload);
        if (payload.length >= 4) req.put("op", buf.getInt());
        if (payload.length >= 12) req.put("targetRoleId", buf.getLong());
        if (payload.length >= 16) req.put("combatType", buf.getInt());
        if (payload.length >= 20) req.put("stageId", buf.getInt());
        if (payload.length >= 24) req.put("monsterId", buf.getInt());
        if (payload.length >= 28) req.put("isBoss", buf.getInt() == 1);
        if (payload.length > 28) {
            String sessionId = new String(payload, 28, payload.length - 28, StandardCharsets.UTF_8).trim();
            if (!sessionId.isBlank()) req.put("sessionId", sessionId);
        }
        return req;
    }

    private Map<String, Object> ok(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(int errorCode, String errorType, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("errorCode", errorCode);
        result.put("error", errorType);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> error(String message) {
        return error(0, "ERROR", message);
    }

    private void safeEmitError(PlayerSession session, String message) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(error(message));
            Emitters.emit(session, MessageIds.SC_BATTLE_RESP, json);
        } catch (Exception ignored) {
            // Nothing else we can do here.
        }
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? defaultValue : str;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> listValue)) {
            return List.of();
        }

        List<String> out = new ArrayList<>();
        for (Object item : listValue) {
            if (item != null) {
                String text = String.valueOf(item).trim();
                if (!text.isEmpty()) {
                    out.add(text);
                }
            }
        }
        return out;
    }

    private String resolveCombatType(int combatTypeCode, Map<String, Object> req) {
        String fromRequest = getString(req, "combatTypeName", "");
        if (!fromRequest.isBlank()) {
            return fromRequest;
        }

        return switch (combatTypeCode) {
            case 2 -> "ARENA";
            case 3 -> "TRIAL";
            case 4 -> "DUNGEON";
            case 5 -> "BOSS";
            default -> "PVP";
        };
    }
}

