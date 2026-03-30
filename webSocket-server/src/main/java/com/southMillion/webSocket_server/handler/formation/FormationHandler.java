package com.SouthMillion.webSocket_server.handler.formation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Trận Pháp (Formation) operations.
 *
 * <p>MsgId routing:
 * <ul>
 *   <li>CS 8144 CS_BATTLE_FORMATION_REQ → op=1 save slots, op=2 level-up</li>
 *   <li>SC 8145 SC_BATTLE_FORMATION_ACK ← ACK sent back to client</li>
 *   <li>CS 8146 CS_FORMATION_QUERY_REQ  → query current state</li>
 *   <li>SC 8147 SC_FORMATION_QUERY_ACK  ← state pushed to client</li>
 * </ul>
 *
 * <p>Formation state is stored in Redis:
 * <ul>
 *   <li>{@code formation:level:{roleId}} — current formation level (integer string)</li>
 *   <li>{@code formation:slots:{roleId}} — current saved slot JSON</li>
 * </ul>
 *
 * <p>Task condition:
 * <ul>
 *   <li>condition_27 published on op=2 (level-up) success with the new level as count</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormationHandler implements MessageHandler {

    /** op=1: save formation slots. */
    private static final int OP_SAVE_SLOTS  = 1;
    /** op=2: upgrade formation level. */
    private static final int OP_LEVEL_UP    = 2;
    /** op=0: query formation state (also served via CS_FORMATION_QUERY_REQ 8146). */
    private static final int OP_QUERY       = 0;

    private static final String REDIS_LEVEL_KEY  = "formation:level:";
    private static final String REDIS_SLOTS_KEY  = "formation:slots:";

    private final StringRedisTemplate redis;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping conditionMapping;
    private final ObjectMapper objectMapper;

    @Override
    public int[] interests() {
        return new int[]{MsgIds.CS_BATTLE_FORMATION_REQ, MsgIds.CS_FORMATION_QUERY_REQ};
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            Long roleId = session.getRoleId();
            if (roleId == null) {
                log.warn("[formation] Reject: roleId is null");
                return;
            }
            try {
                if (msgId == MsgIds.CS_FORMATION_QUERY_REQ) {
                    handleQuery(session, roleId);
                    return;
                }
                // CS_BATTLE_FORMATION_REQ (8144) — JSON payload
                Map<String, Object> req = parsePayload(payload);
                int op = getInt(req, "op", OP_QUERY);
                switch (op) {
                    case OP_SAVE_SLOTS -> handleSaveSlots(session, roleId, req);
                    case OP_LEVEL_UP   -> handleLevelUp(session, roleId);
                    default            -> handleQuery(session, roleId);
                }
            } catch (Exception e) {
                log.error("[formation] Error handling msgId={} roleId={}", msgId, roleId, e);
                sendAck(session, 500, 0, null);
            }
        });
    }

    // ── operation handlers ────────────────────────────────────────────────────

    private void handleSaveSlots(PlayerSession session, Long roleId, Map<String, Object> req) {
        int formationId = getInt(req, "formationId", 1);
        Object slots    = req.get("slots");

        try {
            String slotsJson = slots != null ? objectMapper.writeValueAsString(slots) : "[]";
            redis.opsForValue().set(REDIS_SLOTS_KEY + roleId, slotsJson);
        } catch (Exception e) {
            log.error("[formation] Failed to save slots roleId={}", roleId, e);
            sendAck(session, 500, formationId, null);
            return;
        }

        int level = currentLevel(roleId);
        log.info("[formation] Saved slots formationId={} roleId={}", formationId, roleId);
        sendAck(session, 0, formationId, level);
    }

    private void handleLevelUp(PlayerSession session, Long roleId) {
        // Increment level in Redis atomically
        Long newLevelLong = redis.opsForValue().increment(REDIS_LEVEL_KEY + roleId);
        int newLevel = newLevelLong != null ? newLevelLong.intValue() : 1;

        // Publish condition_27 with updated level as task progress value
        String taskKey = conditionMapping.formationLevelUpTaskKey();
        if (taskKey != null) {
            taskProgressPublisher.publish(roleId, taskKey, newLevel, "websocket-formation-levelup");
        }

        log.info("[formation] LevelUp roleId={} newLevel={}", roleId, newLevel);
        sendAck(session, 0, 0, newLevel);
    }

    private void handleQuery(PlayerSession session, Long roleId) {
        int level   = currentLevel(roleId);
        String slotsJson = redis.opsForValue().get(REDIS_SLOTS_KEY + roleId);
        Map<String, Object> data = new HashMap<>();
        data.put("retCode",     0);
        data.put("level",       level);
        data.put("formationId", 1);
        data.put("slots",       slotsJson != null ? slotsJson : "[]");
        emitJson(session, MsgIds.SC_FORMATION_QUERY_ACK, data);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private int currentLevel(Long roleId) {
        String raw = redis.opsForValue().get(REDIS_LEVEL_KEY + roleId);
        if (raw == null) return 0;
        try { return Integer.parseInt(raw); } catch (NumberFormatException e) { return 0; }
    }

    private void sendAck(PlayerSession session, int retCode, int formationId, Integer level) {
        Map<String, Object> data = new HashMap<>();
        data.put("retCode",     retCode);
        data.put("formationId", formationId);
        if (level != null) data.put("level", level);
        emitJson(session, MsgIds.SC_BATTLE_FORMATION_ACK, data);
    }

    private void emitJson(PlayerSession session, int msgId, Map<String, Object> data) {
        try {
            Emitters.emit(session, msgId, objectMapper.writeValueAsBytes(data));
        } catch (Exception e) {
            log.error("[formation] Failed to emit msgId={} roleId={}", msgId, session.getRoleId(), e);
        }
    }

    private Map<String, Object> parsePayload(byte[] payload) {
        if (payload == null || payload.length == 0) return Map.of();
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception ignored) {}
        return def;
    }
}
