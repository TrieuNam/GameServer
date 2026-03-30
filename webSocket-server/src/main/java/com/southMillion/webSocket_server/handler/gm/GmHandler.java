package com.SouthMillion.webSocket_server.handler.gm;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.GmFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msggm.Msggm;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles GM (Game Master) commands from a trusted GM client.
 *
 * Proto: PB_CSGMCommand (2001) — bytes type + bytes command (JSON payload)
 * Response: PB_SCGMCommand (2000) — bytes type + bytes result (JSON)
 *
 * Command types (type field, case-insensitive):
 *   give_item, remove_item, add_currency, deduct_currency,
 *   update_vip, ban, unban, broadcast, get_player
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GmHandler implements MessageHandler {

    private final GmFeign gmFeign;
    private final ObjectMapper objectMapper;

    @Override
    public int[] interests() {
        return new int[]{2001}; // PB_CSGMCommand
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            String type = "";
            try {
                Msggm.PB_CSGMCommand req = Msggm.PB_CSGMCommand.parseFrom(payload);
                type = req.hasType() ? req.getType().toStringUtf8() : "";
                String command = req.hasCommand() ? req.getCommand().toStringUtf8() : "{}";
                Long roleId = session.getRoleId();

                log.info("[GM] command={}, roleId={}", type, roleId);

                Map<String, Object> params = parseCommand(command);
                params.putIfAbsent("roleId", roleId);

                Map<String, Object> result = dispatch(type, params);
                sendResponse(session, type, result);

            } catch (Exception e) {
                log.error("[GM] Error for roleId={}", session.getRoleId(), e);
                sendResponse(session, type, Map.of("error", "internal_error"));
            }
        });
    }

    private Map<String, Object> parseCommand(String command) {
        try {
            return objectMapper.readValue(command, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<String, Object> dispatch(String type, Map<String, Object> params) {
        try {
            return switch (type.toLowerCase()) {
                case "give_item"       -> gmFeign.giveItem(params);
                case "remove_item"     -> gmFeign.removeItem(params);
                case "add_currency"    -> gmFeign.addCurrency(params);
                case "deduct_currency" -> gmFeign.deductCurrency(params);
                case "update_vip"      -> gmFeign.updateVipLevel(params);
                case "broadcast"       -> gmFeign.broadcastMessage(params);
                case "get_player"      -> {
                    Object pid = params.getOrDefault("playerId", params.get("roleId"));
                    yield gmFeign.getPlayerDetails(pid != null ? pid.toString() : "0");
                }
                case "ban" -> {
                    long userId = toLong(params.get("userId"));
                    String reason = (String) params.getOrDefault("reason", "GM ban");
                    Integer days = params.containsKey("durationDays")
                            ? toInt(params.get("durationDays")) : null;
                    yield gmFeign.banUser(userId, reason, days);
                }
                case "unban" -> {
                    long userId = toLong(params.get("userId"));
                    String reason = (String) params.get("reason");
                    yield gmFeign.unbanUser(userId, reason);
                }
                default -> {
                    log.warn("[GM] Unknown command type: {}", type);
                    yield Map.of("error", "unknown_command", "type", type);
                }
            };
        } catch (Exception e) {
            log.error("[GM] Dispatch failed for type={}", type, e);
            return Map.of("error", e.getMessage() != null ? e.getMessage() : "dispatch_error");
        }
    }

    private void sendResponse(PlayerSession session, String type, Map<String, Object> result) {
        try {
            String resultJson = objectMapper.writeValueAsString(result != null ? result : Map.of());
            Msggm.PB_SCGMCommand resp = Msggm.PB_SCGMCommand.newBuilder()
                    .setType(ByteString.copyFromUtf8(type))
                    .setResult(ByteString.copyFromUtf8(resultJson))
                    .build();
            Emitters.emit(session, 2000, resp.toByteArray());
        } catch (Exception e) {
            log.error("[GM] sendResponse failed", e);
        }
    }

    private long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v != null) return Long.parseLong(v.toString());
        return 0L;
    }

    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v != null) return Integer.parseInt(v.toString());
        return 0;
    }
}
